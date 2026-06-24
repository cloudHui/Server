package web.handler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import msg.registor.message.GMsg;
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

import com.fasterxml.jackson.databind.ObjectMapper;

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

	public GameWebSocketHandler(UserService userService, GateClient gateClient) {
		this.userService = userService;
		this.gateClient = gateClient;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		logger.info("WebSocket连接建立, wsSessionId: {}", session.getId());
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		String sessionId = sessionMapping.remove(session.getId());
		if (sessionId != null) {
			logger.info("WebSocket连接关闭, sessionId: {}, status: {}", sessionId, status);
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		String action = "unknown";
		int seq = 0;
		try {
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

	/**
	 * 认证 - 绑定WebSocket到用户会话
	 */
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

		sendResponse(wsSession, "auth", seq, 0, "认证成功", null);
		logger.info("WebSocket认证成功, userId: {}, sessionId: {}", user.getUserId(), sessionId);
	}

	/**
	 * 进入桌子
	 */
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
					java.util.Map<String, Object> resultData = new java.util.HashMap<>();
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

	/**
	 * 游戏操作（出牌、叫地主等）
	 */
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

		// 处理出牌 - 将牌值放入单个CardInfo中
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
					java.util.Map<String, Object> resultData = new java.util.HashMap<>();
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

	/**
	 * 离开桌子
	 */
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

	// ==================== 辅助方法 ====================

	private String getSessionId(WebSocketSession wsSession) {
		return sessionMapping.get(wsSession.getId());
	}

	/** 发送JSON响应（同步，WebSocketSession.sendMessage非线程安全） */
	private void sendResponse(WebSocketSession session, String action, int seq, int code, String msg, Object data) {
		try {
			java.util.Map<String, Object> response = new java.util.HashMap<>();
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

	private java.util.List<java.util.Map<String, Object>> formatPlayers(List<GameProto.Player> players, int currentRoleId) {
		java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
		for (GameProto.Player player : players) {
			java.util.Map<String, Object> p = new java.util.HashMap<>();
			p.put("roleId", player.getRoleId());
			p.put("position", player.getPosition());
			p.put("nickName", player.getNickName().toStringUtf8());
			p.put("cardCount", player.getCardsCount());

			// 只有自己的牌有值（协议约定：自己的牌是真实值，其他人的牌值为0）
			if (player.getRoleId() == currentRoleId && player.getCardsCount() > 0) {
				java.util.List<Integer> cardValues = new java.util.ArrayList<>();
				for (GameProto.Card card : player.getCardsList()) {
					cardValues.add(card.getValue());
				}
				p.put("cards", cardValues);
			}
			result.add(p);
		}
		return result;
	}

	private java.util.Map<String, Object> formatTableInfo(GameProto.TableInfo tableInfo) {
		java.util.Map<String, Object> result = new java.util.HashMap<>();
		result.put("roomId", tableInfo.getRoomId());
		result.put("tableId", tableInfo.getTableId());
		result.put("landlord", tableInfo.getLandlord());
		return result;
	}
}
