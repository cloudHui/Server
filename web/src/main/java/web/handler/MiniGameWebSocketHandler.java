package web.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import web.minigame.ChessBoard;
import web.minigame.GomokuBoard;
import web.minigame.MiniRoom;
import web.service.UserService;

/**
 * 休闲小游戏 WebSocket：五子棋 / 象棋匹配与对战。
 * 消息: {"action":"...","seq":1,"data":{...}}
 */
@Component
public class MiniGameWebSocketHandler extends TextWebSocketHandler {
	private static final Logger logger = LoggerFactory.getLogger(MiniGameWebSocketHandler.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final UserService userService;

	private final Map<String, String> wsToSession = new ConcurrentHashMap<>();
	private final Map<String, WebSocketSession> sessionToWs = new ConcurrentHashMap<>();
	private final Map<String, MiniRoom> rooms = new ConcurrentHashMap<>();
	private final Map<String, String> sessionRoom = new ConcurrentHashMap<>();

	private final Object queueLock = new Object();
	private final List<QueueEntry> gomokuQueue = new ArrayList<>();
	private final List<QueueEntry> chessQueue = new ArrayList<>();

	public MiniGameWebSocketHandler(UserService userService) {
		this.userService = userService;
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		String sid = wsToSession.remove(session.getId());
		if (sid == null) {
			return;
		}
		sessionToWs.remove(sid, session);
		leaveQueue(sid);
		String roomId = sessionRoom.remove(sid);
		if (roomId != null) {
			MiniRoom room = rooms.get(roomId);
			if (room != null) {
				handleDisconnect(room, sid);
			}
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		int seq = 0;
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> msg = objectMapper.readValue(message.getPayload(), Map.class);
			String action = (String) msg.get("action");
			seq = msg.get("seq") != null ? ((Number) msg.get("seq")).intValue() : 0;
			@SuppressWarnings("unchecked")
			Map<String, Object> data = (Map<String, Object>) msg.get("data");
			if (data == null) {
				data = new HashMap<>();
			}

			switch (action == null ? "" : action) {
				case "auth":
					handleAuth(session, seq, data);
					break;
				case "match":
					handleMatch(session, seq, data);
					break;
				case "cancelMatch":
					handleCancelMatch(session, seq);
					break;
				case "move":
					handleMove(session, seq, data);
					break;
				case "resign":
					handleResign(session, seq);
					break;
				case "leave":
					handleLeave(session, seq);
					break;
				default:
					sendError(session, seq, "未知操作: " + action);
			}
		} catch (Exception e) {
			logger.error("处理小游戏消息失败", e);
			sendError(session, seq, "消息处理失败");
		}
	}

	private void handleAuth(WebSocketSession ws, int seq, Map<String, Object> data) {
		String sessionId = (String) data.get("sessionId");
		if (sessionId == null) {
			sendError(ws, seq, "缺少 sessionId");
			return;
		}
		UserService.UserInfo user = userService.getSession(sessionId);
		if (user == null) {
			sendError(ws, seq, "会话无效");
			return;
		}
		wsToSession.put(ws.getId(), sessionId);
		WebSocketSession old = sessionToWs.put(sessionId, ws);
		if (old != null && old.isOpen() && old != ws) {
			try {
				old.close(CloseStatus.NORMAL);
			} catch (IOException ignored) {
			}
		}
		Map<String, Object> payload = new HashMap<>();
		payload.put("userId", user.getUserId());
		payload.put("nickname", user.getNickname());
		sendOk(ws, "auth", seq, "认证成功", payload);
	}

	private void handleMatch(WebSocketSession ws, int seq, Map<String, Object> data) {
		String sid = requireAuth(ws, seq);
		if (sid == null) {
			return;
		}
		if (sessionRoom.containsKey(sid)) {
			sendError(ws, seq, "已在对局中");
			return;
		}
		String game = data.get("game") == null ? "" : String.valueOf(data.get("game"));
		MiniRoom.GameType type;
		if ("gomoku".equalsIgnoreCase(game)) {
			type = MiniRoom.GameType.GOMOKU;
		} else if ("chess".equalsIgnoreCase(game)) {
			type = MiniRoom.GameType.CHESS;
		} else {
			sendError(ws, seq, "不支持的游戏: " + game);
			return;
		}
		UserService.UserInfo user = userService.getSession(sid);
		if (user == null) {
			sendError(ws, seq, "会话无效");
			return;
		}

		MiniRoom matched = null;
		synchronized (queueLock) {
			leaveQueueLocked(sid);
			List<QueueEntry> queue = type == MiniRoom.GameType.GOMOKU ? gomokuQueue : chessQueue;
			QueueEntry peer = null;
			Iterator<QueueEntry> it = queue.iterator();
			while (it.hasNext()) {
				QueueEntry e = it.next();
				if (e.sessionId.equals(sid)) {
					it.remove();
					continue;
				}
				if (e.userId == user.getUserId()) {
					continue;
				}
				WebSocketSession peerWs = sessionToWs.get(e.sessionId);
				if (peerWs == null || !peerWs.isOpen()) {
					it.remove();
					continue;
				}
				peer = e;
				it.remove();
				break;
			}
			if (peer != null) {
				matched = new MiniRoom(type, peer.sessionId, peer.userId, peer.name,
						sid, user.getUserId(), displayName(user));
				rooms.put(matched.getRoomId(), matched);
				sessionRoom.put(matched.getPlayerASession(), matched.getRoomId());
				sessionRoom.put(matched.getPlayerBSession(), matched.getRoomId());
			} else {
				queue.add(new QueueEntry(sid, user.getUserId(), displayName(user)));
			}
		}

		if (matched == null) {
			sendOk(ws, "match", seq, "排队中", mapOf("status", "queued", "game", game));
			return;
		}
		sendOk(ws, "match", seq, "匹配成功", mapOf("status", "matched"));
		notifyMatched(matched);
	}

	private void handleCancelMatch(WebSocketSession ws, int seq) {
		String sid = requireAuth(ws, seq);
		if (sid == null) {
			return;
		}
		leaveQueue(sid);
		sendOk(ws, "cancelMatch", seq, "已取消", null);
	}

	private void handleMove(WebSocketSession ws, int seq, Map<String, Object> data) {
		String sid = requireAuth(ws, seq);
		if (sid == null) {
			return;
		}
		String roomId = sessionRoom.get(sid);
		if (roomId == null) {
			sendError(ws, seq, "不在对局中");
			return;
		}
		MiniRoom room = rooms.get(roomId);
		if (room == null) {
			sendError(ws, seq, "房间不存在");
			return;
		}

		boolean ok;
		Map<String, Object> movePayload = new HashMap<>();
		if (room.getGameType() == MiniRoom.GameType.GOMOKU) {
			Number xNum = (Number) data.get("x");
			Number yNum = (Number) data.get("y");
			if (xNum == null || yNum == null) {
				sendError(ws, seq, "缺少坐标");
				return;
			}
			int color = room.isSideA(sid) ? GomokuBoard.BLACK : GomokuBoard.WHITE;
			ok = room.getGomoku().place(xNum.intValue(), yNum.intValue(), color);
			if (!ok) {
				sendError(ws, seq, "非法落子");
				return;
			}
			movePayload.put("x", xNum.intValue());
			movePayload.put("y", yNum.intValue());
			movePayload.put("color", color);
			movePayload.put("turn", room.getGomoku().getTurn());
			movePayload.put("finished", room.getGomoku().isFinished());
			movePayload.put("winner", room.getGomoku().getWinner());
		} else {
			Number fr = (Number) data.get("fr");
			Number fc = (Number) data.get("fc");
			Number tr = (Number) data.get("tr");
			Number tc = (Number) data.get("tc");
			if (fr == null || fc == null || tr == null || tc == null) {
				sendError(ws, seq, "缺少走法");
				return;
			}
			boolean asRed = room.isSideA(sid);
			ok = room.getChess().move(fr.intValue(), fc.intValue(), tr.intValue(), tc.intValue(), asRed);
			if (!ok) {
				sendError(ws, seq, "非法走法");
				return;
			}
			movePayload.put("fr", fr.intValue());
			movePayload.put("fc", fc.intValue());
			movePayload.put("tr", tr.intValue());
			movePayload.put("tc", tc.intValue());
			movePayload.put("board", room.getChess().boardString());
			movePayload.put("redTurn", room.getChess().isRedTurn());
			movePayload.put("finished", room.getChess().isFinished());
			movePayload.put("winner", room.getChess().getWinner());
			movePayload.put("reason", room.getChess().getEndReason());
		}

		sendOk(ws, "move", seq, "ok", movePayload);
		broadcast(room, "move", movePayload, sid);

		if (room.getGameType() == MiniRoom.GameType.GOMOKU && room.getGomoku().isFinished()) {
			finishRoom(room, gomokuResult(room));
		} else if (room.getGameType() == MiniRoom.GameType.CHESS && room.getChess().isFinished()) {
			finishRoom(room, chessResult(room));
		}
	}

	private void handleResign(WebSocketSession ws, int seq) {
		String sid = requireAuth(ws, seq);
		if (sid == null) {
			return;
		}
		MiniRoom room = currentRoom(sid);
		if (room == null) {
			sendError(ws, seq, "不在对局中");
			return;
		}
		if (room.getGameType() == MiniRoom.GameType.GOMOKU) {
			GomokuBoard b = room.getGomoku();
			if (b.isFinished()) {
				sendError(ws, seq, "对局已结束");
				return;
			}
			// 认输：对手获胜
			int winner = room.isSideA(sid) ? GomokuBoard.WHITE : GomokuBoard.BLACK;
			Map<String, Object> result = new HashMap<>();
			result.put("winner", winner);
			result.put("reason", "认输");
			result.put("finished", true);
			sendOk(ws, "resign", seq, "ok", null);
			finishRoom(room, result);
			return;
		}
		ChessBoard c = room.getChess();
		if (c.isFinished()) {
			sendError(ws, seq, "对局已结束");
			return;
		}
		c.resign(room.isSideA(sid));
		sendOk(ws, "resign", seq, "ok", null);
		finishRoom(room, chessResult(room));
	}

	private void handleLeave(WebSocketSession ws, int seq) {
		String sid = requireAuth(ws, seq);
		if (sid == null) {
			return;
		}
		leaveQueue(sid);
		String roomId = sessionRoom.remove(sid);
		if (roomId != null) {
			MiniRoom room = rooms.get(roomId);
			if (room != null) {
				handleDisconnect(room, sid);
			}
		}
		sendOk(ws, "leave", seq, "ok", null);
	}

	private void handleDisconnect(MiniRoom room, String sid) {
		if (!rooms.containsKey(room.getRoomId())) {
			return;
		}
		Map<String, Object> result = new HashMap<>();
		result.put("reason", "对手离开");
		result.put("finished", true);
		if (room.getGameType() == MiniRoom.GameType.GOMOKU) {
			if (!room.getGomoku().isFinished()) {
				result.put("winner", room.isSideA(sid) ? GomokuBoard.WHITE : GomokuBoard.BLACK);
				finishRoom(room, result);
			} else {
				cleanupRoom(room);
			}
		} else {
			if (!room.getChess().isFinished()) {
				room.getChess().resign(room.isSideA(sid));
				finishRoom(room, chessResult(room));
			} else {
				cleanupRoom(room);
			}
		}
	}

	private void finishRoom(MiniRoom room, Map<String, Object> result) {
		broadcast(room, "gameOver", result, null);
		cleanupRoom(room);
	}

	private void cleanupRoom(MiniRoom room) {
		rooms.remove(room.getRoomId());
		sessionRoom.remove(room.getPlayerASession(), room.getRoomId());
		sessionRoom.remove(room.getPlayerBSession(), room.getRoomId());
	}

	private void notifyMatched(MiniRoom room) {
		Map<String, Object> forA = baseMatchInfo(room);
		forA.put("side", room.getGameType() == MiniRoom.GameType.GOMOKU ? "black" : "red");
		forA.put("youAreA", true);
		forA.put("opponent", room.getPlayerBName());
		forA.put("opponentId", room.getPlayerBUserId());

		Map<String, Object> forB = baseMatchInfo(room);
		forB.put("side", room.getGameType() == MiniRoom.GameType.GOMOKU ? "white" : "black");
		forB.put("youAreA", false);
		forB.put("opponent", room.getPlayerAName());
		forB.put("opponentId", room.getPlayerAUserId());

		if (room.getGameType() == MiniRoom.GameType.GOMOKU) {
			forA.put("board", room.getGomoku().snapshot());
			forA.put("turn", room.getGomoku().getTurn());
			forB.put("board", room.getGomoku().snapshot());
			forB.put("turn", room.getGomoku().getTurn());
		} else {
			forA.put("board", room.getChess().boardString());
			forA.put("redTurn", room.getChess().isRedTurn());
			forB.put("board", room.getChess().boardString());
			forB.put("redTurn", room.getChess().isRedTurn());
		}

		sendEvent(sessionToWs.get(room.getPlayerASession()), "matched", forA);
		sendEvent(sessionToWs.get(room.getPlayerBSession()), "matched", forB);
	}

	private Map<String, Object> baseMatchInfo(MiniRoom room) {
		Map<String, Object> m = new HashMap<>();
		m.put("roomId", room.getRoomId());
		m.put("game", room.getGameType() == MiniRoom.GameType.GOMOKU ? "gomoku" : "chess");
		return m;
	}

	private Map<String, Object> gomokuResult(MiniRoom room) {
		Map<String, Object> m = new HashMap<>();
		m.put("winner", room.getGomoku().getWinner());
		m.put("finished", true);
		m.put("reason", room.getGomoku().getWinner() == 0 ? "和棋" : "五子连珠");
		return m;
	}

	private Map<String, Object> chessResult(MiniRoom room) {
		Map<String, Object> m = new HashMap<>();
		m.put("winner", room.getChess().getWinner());
		m.put("finished", true);
		m.put("reason", room.getChess().getEndReason());
		m.put("board", room.getChess().boardString());
		return m;
	}

	private void broadcast(MiniRoom room, String action, Map<String, Object> data, String exceptSession) {
		if (!room.getPlayerASession().equals(exceptSession)) {
			sendEvent(sessionToWs.get(room.getPlayerASession()), action, data);
		}
		if (!room.getPlayerBSession().equals(exceptSession)) {
			sendEvent(sessionToWs.get(room.getPlayerBSession()), action, data);
		}
	}

	private MiniRoom currentRoom(String sid) {
		String roomId = sessionRoom.get(sid);
		return roomId == null ? null : rooms.get(roomId);
	}

	private void leaveQueue(String sid) {
		synchronized (queueLock) {
			leaveQueueLocked(sid);
		}
	}

	private void leaveQueueLocked(String sid) {
		gomokuQueue.removeIf(e -> e.sessionId.equals(sid));
		chessQueue.removeIf(e -> e.sessionId.equals(sid));
	}

	private String requireAuth(WebSocketSession ws, int seq) {
		String sid = wsToSession.get(ws.getId());
		if (sid == null) {
			sendError(ws, seq, "请先认证");
			return null;
		}
		if (userService.getSession(sid) == null) {
			sendError(ws, seq, "会话无效");
			return null;
		}
		return sid;
	}

	private static String displayName(UserService.UserInfo user) {
		if (user.getNickname() != null && !user.getNickname().isEmpty()) {
			return user.getNickname();
		}
		return user.getUsername();
	}

	private static Map<String, Object> mapOf(Object... kv) {
		Map<String, Object> m = new HashMap<>();
		for (int i = 0; i + 1 < kv.length; i += 2) {
			m.put(String.valueOf(kv[i]), kv[i + 1]);
		}
		return m;
	}

	private void sendOk(WebSocketSession ws, String action, int seq, String msg, Map<String, Object> data) {
		Map<String, Object> resp = new HashMap<>();
		resp.put("action", action);
		resp.put("seq", seq);
		resp.put("code", 0);
		resp.put("msg", msg);
		if (data != null) {
			resp.put("data", data);
		}
		write(ws, resp);
	}

	private void sendError(WebSocketSession ws, int seq, String msg) {
		Map<String, Object> resp = new HashMap<>();
		resp.put("action", "error");
		resp.put("seq", seq);
		resp.put("code", -1);
		resp.put("msg", msg);
		write(ws, resp);
	}

	private void sendEvent(WebSocketSession ws, String action, Map<String, Object> data) {
		if (ws == null || !ws.isOpen()) {
			return;
		}
		Map<String, Object> resp = new HashMap<>();
		resp.put("action", action);
		resp.put("seq", 0);
		resp.put("code", 0);
		resp.put("data", data);
		write(ws, resp);
	}

	private void write(WebSocketSession ws, Map<String, Object> resp) {
		if (ws == null || !ws.isOpen()) {
			return;
		}
		try {
			synchronized (ws) {
				ws.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
			}
		} catch (IOException e) {
			logger.warn("发送小游戏消息失败: {}", e.getMessage());
		}
	}

	private static class QueueEntry {
		final String sessionId;
		final int userId;
		final String name;

		QueueEntry(String sessionId, int userId, String name) {
			this.sessionId = sessionId;
			this.userId = userId;
			this.name = name;
		}
	}
}
