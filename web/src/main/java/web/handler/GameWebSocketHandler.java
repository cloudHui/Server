package web.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import msg.registor.HandleTypeRegister;
import msg.registor.message.GMsg;
import net.message.TCPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import proto.ConstProto;
import proto.GameProto;
import web.service.GateClient;
import web.service.UserService;

/**
 * 游戏WebSocket处理器
 * 浏览器通过WebSocket与游戏服务器通信
 * 消息格式: {"action":"xxx","seq":1,"data":{...}}
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
	private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final UserService userService;
	private final GateClient gateClient;

	/** WebSocketSessionId -> sessionId */
	private final Map<String, String> sessionMapping = new ConcurrentHashMap<>();
	/** sessionId -> WebSocketSession（推送用） */
	private final Map<String, WebSocketSession> wsBySession = new ConcurrentHashMap<>();

	public GameWebSocketHandler(UserService userService, GateClient gateClient) {
		this.userService = userService;
		this.gateClient = gateClient;
	}

	@PostConstruct
	public void init() {
		gateClient.setPushListener(this::onGatePush);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		logger.info("WebSocket连接建立, wsSessionId: {}", session.getId());
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		String sessionId = sessionMapping.remove(session.getId());
		if (sessionId != null) {
			wsBySession.remove(sessionId, session);
			logger.info("WebSocket连接关闭, sessionId: {}, status: {}", sessionId, status);
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		String action = "unknown";
		int seq = 0;
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> msg = objectMapper.readValue(message.getPayload(), Map.class);
			action = (String) msg.get("action");
			seq = msg.get("seq") != null ? ((Number) msg.get("seq")).intValue() : 0;
			@SuppressWarnings("unchecked")
			Map<String, Object> data = (Map<String, Object>) msg.get("data");

			logger.debug("收到WebSocket消息, action: {}, seq: {}, sessionId: {}", action, seq, session.getId());

			switch (action) {
				case "auth":
					handleAuth(session, seq, data);
					break;
				case "enterTable":
					handleEnterTable(session, seq, data);
					break;
				case "op":
					handleOp(session, seq, data);
					break;
				case "leave":
					handleLeave(session, seq);
					break;
				default:
					sendError(session, seq, "未知操作: " + action);
			}
		} catch (Exception e) {
			logger.error("处理WebSocket消息异常, action: {}, seq: {}, sessionId: {}", action, seq, session.getId(), e);
			sendError(session, seq, "消息处理失败");
		}
	}

	private void handleAuth(WebSocketSession wsSession, int seq, Map<String, Object> data) {
		String sessionId = (String) data.get("sessionId");
		if (sessionId == null) {
			sendError(wsSession, seq, "缺少sessionId");
			return;
		}

		UserService.UserInfo user = userService.getSession(sessionId);
		if (user == null) {
			sendError(wsSession, seq, "会话无效");
			return;
		}

		sessionMapping.put(wsSession.getId(), sessionId);
		wsBySession.put(sessionId, wsSession);

		sendResponse(wsSession, "auth", seq, 0, "认证成功", null);
		logger.info("WebSocket认证成功, userId: {}, sessionId: {}", user.getUserId(), sessionId);
	}

	private void handleEnterTable(WebSocketSession wsSession, int seq, Map<String, Object> data) {
		String sessionId = getSessionId(wsSession);
		if (sessionId == null) {
			sendError(wsSession, seq, "请先认证");
			return;
		}

		Number tableIdNum = (Number) data.get("tableId");
		if (tableIdNum == null) {
			sendError(wsSession, seq, "缺少tableId");
			return;
		}

		long tableId = tableIdNum.longValue();
		UserService.UserInfo user = userService.getSession(sessionId);

		GameProto.ReqEnterTable request = GameProto.ReqEnterTable.newBuilder()
				.setTableId(tableId)
				.setNick(ByteString.copyFromUtf8(user.getNickname()))
				.build();

		CompletableFuture<Message> future = gateClient.sendAndWait(
				sessionId, GMsg.REQ_ENTER_TABLE_MSG, request, 5);

		future.whenComplete((response, error) -> {
			if (error != null) {
				sendError(wsSession, seq, "进入桌子超时");
				return;
			}

			try {
				if (response instanceof GameProto.AckEnterTable) {
					GameProto.AckEnterTable ack = (GameProto.AckEnterTable) response;
					if (!ack.hasTableInfo() || ack.getTableInfo().getTableId() == 0) {
						sendError(wsSession, seq, "进入桌子失败（座位已满或状态不允许）");
						return;
					}
					Map<String, Object> resultData = new HashMap<>();
					resultData.put("players", formatPlayers(ack.getPlayersList(), user.getUserId()));
					resultData.put("tableInfo", formatTableInfo(ack.getTableInfo()));
					sendResponse(wsSession, "enterTable", seq, 0, "success", resultData);
				} else {
					sendError(wsSession, seq, "进入桌子失败");
				}
			} catch (Exception e) {
				logger.error("处理进入桌子响应异常", e);
				sendError(wsSession, seq, "处理响应失败");
			}
		});
	}

	private void handleOp(WebSocketSession wsSession, int seq, Map<String, Object> data) {
		String sessionId = getSessionId(wsSession);
		if (sessionId == null) {
			sendError(wsSession, seq, "请先认证");
			return;
		}

		Number opChoice = (Number) data.get("choice");
		if (opChoice == null) {
			sendError(wsSession, seq, "缺少choice");
			return;
		}

		ConstProto.Operation opEnum = ConstProto.Operation.forNumber(opChoice.intValue());
		if (opEnum == null) {
			sendError(wsSession, seq, "无效操作类型: " + opChoice);
			return;
		}
		GameProto.OpInfo.Builder opBuilder = GameProto.OpInfo.newBuilder()
				.setChoice(opEnum);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> cards = (List<Map<String, Object>>) data.get("cards");
		if (cards != null) {
			GameProto.CardInfo.Builder cardInfo = GameProto.CardInfo.newBuilder();
			for (Map<String, Object> card : cards) {
				Number value = (Number) card.get("value");
				if (value != null) {
					cardInfo.addCards(GameProto.Card.newBuilder().setValue(value.intValue()).build());
				}
			}
			opBuilder.addOpCards(cardInfo.build());
		}

		GameProto.ReqOp request = GameProto.ReqOp.newBuilder()
				.setOp(opBuilder.build())
				.build();

		CompletableFuture<Message> future = gateClient.sendAndWait(
				sessionId, GMsg.REQ_OP, request, 5);

		future.whenComplete((response, error) -> {
			if (error != null) {
				sendError(wsSession, seq, "操作超时");
				return;
			}

			try {
				if (response instanceof GameProto.AckOp) {
					GameProto.AckOp ack = (GameProto.AckOp) response;
					Map<String, Object> resultData = new HashMap<>();
					resultData.put("opId", ack.getOpId());
					resultData.put("choice", ack.getOp().getChoiceValue());
					sendResponse(wsSession, "op", seq, 0, "success", resultData);
				} else {
					sendError(wsSession, seq, "操作失败");
				}
			} catch (Exception e) {
				logger.error("处理操作响应异常", e);
				sendError(wsSession, seq, "处理响应失败");
			}
		});
	}

	private void handleLeave(WebSocketSession wsSession, int seq) {
		String sessionId = getSessionId(wsSession);
		if (sessionId == null) {
			sendError(wsSession, seq, "请先认证");
			return;
		}

		GameProto.ReqLeaveTable request = GameProto.ReqLeaveTable.newBuilder().build();

		CompletableFuture<Message> future = gateClient.sendAndWait(
				sessionId, GMsg.REQ_LEAVE, request, 5);

		future.whenComplete((response, error) -> {
			if (error != null) {
				sendError(wsSession, seq, "离开桌子超时");
				return;
			}
			sendResponse(wsSession, "leave", seq, 0, "success", null);
		});
	}

	/** Gate 推送 → WebSocket */
	private void onGatePush(String sessionId, TCPMessage tcpMessage) {
		WebSocketSession ws = wsBySession.get(sessionId);
		if (ws == null || !ws.isOpen()) {
			return;
		}
		try {
			int msgId = tcpMessage.getMessageId();
			Message proto = HandleTypeRegister.parseMessage(msgId,
					tcpMessage.getMessage() == null ? new byte[0] : tcpMessage.getMessage());
			String action = pushAction(msgId);
			if (action == null) {
				return;
			}
			Object data = formatPush(msgId, proto);
			sendResponse(ws, action, 0, 0, "push", data);
		} catch (Exception e) {
			logger.error("转发推送失败, sessionId: {}, msgId: 0x{}",
					sessionId, Integer.toHexString(tcpMessage.getMessageId()), e);
		}
	}

	private static String pushAction(int msgId) {
		if (msgId == GMsg.NOT_CARD) return "notCard";
		if (msgId == GMsg.NOT_OP) return "notOp";
		if (msgId == GMsg.NOT_STATE || msgId == GMsg.NOT_TABLE_STATE) return "notState";
		if (msgId == GMsg.NOT_RESULT) return "notResult";
		if (msgId == GMsg.MJ_TILE_NOT) return "notMjState";
		if (msgId == GMsg.NOT_ROUND_RESULT) return "notRoundResult";
		if (msgId == GMsg.NOT_GAME_RESULT) return "notGameResult";
		return null;
	}

	private Object formatPush(int msgId, Message proto) {
		if (proto instanceof GameProto.NotCard) {
			return formatNotCard((GameProto.NotCard) proto);
		}
		if (proto instanceof GameProto.NotOperation) {
			return formatNotOp((GameProto.NotOperation) proto);
		}
		if (proto instanceof GameProto.NotTableState) {
			GameProto.NotTableState n = (GameProto.NotTableState) proto;
			Map<String, Object> m = new HashMap<>();
			m.put("state", n.getState());
			return m;
		}
		if (proto instanceof GameProto.NotResult) {
			return formatNotResult((GameProto.NotResult) proto);
		}
		if (proto instanceof GameProto.NotMjState) {
			return formatNotMjState((GameProto.NotMjState) proto);
		}
		if (proto instanceof GameProto.NotRoundResult) {
			return formatNotRoundResult((GameProto.NotRoundResult) proto);
		}
		if (proto instanceof GameProto.NotGameResult) {
			return formatNotGameResult((GameProto.NotGameResult) proto);
		}
		return new HashMap<>();
	}

	private Map<String, Object> formatNotCard(GameProto.NotCard n) {
		Map<String, Object> m = new HashMap<>();
		List<Map<String, Object>> nCards = new ArrayList<>();
		for (GameProto.NCardsInfo info : n.getNCardsList()) {
			Map<String, Object> c = new HashMap<>();
			c.put("roleId", info.getRoleId());
			List<Map<String, Object>> cards = new ArrayList<>();
			for (GameProto.Card card : info.getCardsList()) {
				Map<String, Object> cv = new HashMap<>();
				cv.put("value", card.getValue());
				cards.add(cv);
			}
			c.put("cards", cards);
			nCards.add(c);
		}
		m.put("nCards", nCards);
		return m;
	}

	private Map<String, Object> formatNotOp(GameProto.NotOperation n) {
		Map<String, Object> m = new HashMap<>();
		m.put("opSeat", n.getOpSeat());
		m.put("wait", n.getWait());
		m.put("choice", formatOpChoices(n.getChoiceList()));
		return m;
	}

	private Map<String, Object> formatNotResult(GameProto.NotResult n) {
		Map<String, Object> m = new HashMap<>();
		m.put("winner", n.getWinner());
		m.put("landlord_id", n.getLandlordId());
		m.put("win_team", n.getWinTeam());
		m.put("base_score", n.getBaseScore());
		m.put("rob_multiplier", n.getRobMultiplier());
		m.put("spring", n.getSpring());
		m.put("anti_spring", n.getAntiSpring());
		m.put("settle_factor", n.getSettleFactor());
		List<Map<String, Object>> players = new ArrayList<>();
		for (GameProto.RPlayer p : n.getRPlayersList()) {
			Map<String, Object> rp = new HashMap<>();
			rp.put("roleId", p.getRoleId());
			List<Integer> cards = new ArrayList<>();
			for (GameProto.Card c : p.getCardsList()) {
				cards.add(c.getValue());
			}
			rp.put("cards", cards);
			players.add(rp);
		}
		m.put("rPlayers", players);
		return m;
	}

	private Map<String, Object> formatNotMjState(GameProto.NotMjState n) {
		Map<String, Object> m = new HashMap<>();
		m.put("opSeat", n.getOpSeat());
		m.put("tileId", n.getTileId());
		m.put("action", n.getActionValue());
		m.put("wait", n.getWait());
		m.put("wallLeft", n.getWallLeft());
		m.put("choice", formatOpChoices(n.getChoiceList()));
		return m;
	}

	/** 透传操作选项及附属牌（吃 combo / 暗杠补杠目标牌） */
	private List<Map<String, Object>> formatOpChoices(List<GameProto.OpInfo> ops) {
		List<Map<String, Object>> choices = new ArrayList<>();
		for (GameProto.OpInfo op : ops) {
			Map<String, Object> c = new HashMap<>();
			c.put("choice", op.getChoiceValue());
			List<Map<String, Object>> cards = new ArrayList<>();
			for (GameProto.CardInfo cardInfo : op.getOpCardsList()) {
				for (GameProto.Card card : cardInfo.getCardsList()) {
					Map<String, Object> cv = new HashMap<>();
					cv.put("value", card.getValue());
					cards.add(cv);
				}
			}
			if (!cards.isEmpty()) {
				c.put("cards", cards);
			}
			choices.add(c);
		}
		return choices;
	}

	private Map<String, Object> formatNotRoundResult(GameProto.NotRoundResult n) {
		Map<String, Object> m = new HashMap<>();
		m.put("round", n.getRound());
		m.put("winnerSeat", n.getWinnerSeat());
		m.put("fan", n.getFan());
		m.put("winType", n.getWinType().toStringUtf8());
		m.put("winTile", n.getWinTile());
		List<Map<String, Object>> scores = new ArrayList<>();
		for (GameProto.SeatScore s : n.getSeatScoresList()) {
			Map<String, Object> sc = new HashMap<>();
			sc.put("seat", s.getSeat());
			sc.put("score", s.getScore());
			scores.add(sc);
		}
		m.put("seatScores", scores);
		List<Map<String, Object>> hands = new ArrayList<>();
		for (GameProto.HandInfo h : n.getHandsList()) {
			Map<String, Object> hi = new HashMap<>();
			hi.put("seat", h.getSeat());
			hi.put("handTiles", h.getHandTilesList());
			hands.add(hi);
		}
		m.put("hands", hands);
		return m;
	}

	private Map<String, Object> formatNotGameResult(GameProto.NotGameResult n) {
		Map<String, Object> m = new HashMap<>();
		m.put("totalRounds", n.getTotalRounds());
		m.put("completedRounds", n.getCompletedRounds());
		List<Map<String, Object>> totals = new ArrayList<>();
		for (GameProto.SeatScore s : n.getTotalScoresList()) {
			Map<String, Object> sc = new HashMap<>();
			sc.put("seat", s.getSeat());
			sc.put("score", s.getScore());
			totals.add(sc);
		}
		m.put("totalScores", totals);
		List<Map<String, Object>> rounds = new ArrayList<>();
		for (GameProto.RoundSummary r : n.getRoundsList()) {
			Map<String, Object> rs = new HashMap<>();
			rs.put("round", r.getRound());
			rs.put("winnerSeat", r.getWinnerSeat());
			rs.put("fan", r.getFan());
			rs.put("winType", r.getWinType().toStringUtf8());
			rounds.add(rs);
		}
		m.put("rounds", rounds);
		return m;
	}

	private String getSessionId(WebSocketSession wsSession) {
		return sessionMapping.get(wsSession.getId());
	}

	private void sendResponse(WebSocketSession session, String action, int seq, int code, String msg, Object data) {
		try {
			Map<String, Object> response = new HashMap<>();
			response.put("action", action);
			response.put("seq", seq);
			response.put("code", code);
			response.put("msg", msg);
			if (data != null) {
				response.put("data", data);
			}
			String json = objectMapper.writeValueAsString(response);
			synchronized (session) {
				session.sendMessage(new TextMessage(json));
			}
		} catch (Exception e) {
			logger.error("发送WebSocket消息失败, action: {}, sessionId: {}", action, session.getId(), e);
		}
	}

	private void sendError(WebSocketSession session, int seq, String errorMsg) {
		sendResponse(session, "error", seq, -1, errorMsg, null);
	}

	private List<Map<String, Object>> formatPlayers(List<GameProto.Player> players, int currentRoleId) {
		List<Map<String, Object>> result = new ArrayList<>();
		for (GameProto.Player player : players) {
			Map<String, Object> p = new HashMap<>();
			p.put("roleId", player.getRoleId());
			p.put("position", player.getPosition());
			p.put("nickName", player.getNickName().toStringUtf8());
			p.put("cardCount", player.getCardsCount());
			if (player.getRoleId() == currentRoleId && player.getCardsCount() > 0) {
				List<Integer> cardValues = new ArrayList<>();
				for (GameProto.Card card : player.getCardsList()) {
					cardValues.add(card.getValue());
				}
				p.put("cards", cardValues);
			}
			result.add(p);
		}
		return result;
	}

	private Map<String, Object> formatTableInfo(GameProto.TableInfo tableInfo) {
		Map<String, Object> result = new HashMap<>();
		result.put("roomId", tableInfo.getRoomId());
		result.put("tableId", tableInfo.getTableId());
		result.put("landlord", tableInfo.getLandlord());
		return result;
	}
}
