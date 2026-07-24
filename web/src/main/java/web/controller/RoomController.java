package web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proto.ModelProto;
import proto.LobbyProto;
import web.service.UserService;

/**
 * 房间和桌子管理接口
 */
@RestController
@RequestMapping("/api")
public class RoomController {
	private static final Logger logger = LoggerFactory.getLogger(RoomController.class);

	private final UserService userService;
	private final LobbyAdminClient lobbyAdminClient;

	public RoomController(UserService userService, LobbyAdminClient lobbyAdminClient) {
		this.userService = userService;
		this.lobbyAdminClient = lobbyAdminClient;
	}

	/**
	 * 获取房间列表
	 * GET /api/rooms?sessionId=xxx
	 */
	@GetMapping("/rooms")
	public ResponseEntity<Map<String, Object>> getRooms(@RequestParam String sessionId) {
		UserService.UserInfo user = userService.getSession(sessionId);
		if (user == null) {
			return ResponseEntity.ok(errorResponse(401, "会话无效，请重新登录"));
		}

		try {
			CompletableFuture<Message> future = userService.getRoomList(sessionId);
			Message response = future.get(5, TimeUnit.SECONDS);

			if (response instanceof LobbyProto.AckRoomList) {
				LobbyProto.AckRoomList ackRoomList = (LobbyProto.AckRoomList) response;
				List<Map<String, Object>> rooms = new ArrayList<>();

				for (ModelProto.Room room : ackRoomList.getRoomListList()) {
					Map<String, Object> roomData = new HashMap<>();
					roomData.put("roomId", room.getRoomId());
					roomData.put("gameType", room.getGameType());

					List<Map<String, Object>> tables = new ArrayList<>();
					for (ModelProto.RoomTableInfo table : room.getTablesList()) {
						Map<String, Object> tableData = new HashMap<>();
						tableData.put("tableId", table.getTableId());
						tableData.put("stat", table.getStat());
						tableData.put("playerCount", table.getTableRolesCount());

						List<Map<String, Object>> players = new ArrayList<>();
						for (ModelProto.RoomRole role : table.getTableRolesList()) {
							Map<String, Object> playerData = new HashMap<>();
							playerData.put("roleId", role.getRoleId());
							playerData.put("nickName", role.getNickName().toStringUtf8());
							players.add(playerData);
						}
						tableData.put("players", players);
						tableData.put("mine", table.getTableRolesList().stream()
								.anyMatch(role -> role.getRoleId() == user.getUserId()));
						tables.add(tableData);
					}
					roomData.put("tables", tables);
					for (Map<String, Object> table : tables) {
						if (Boolean.TRUE.equals(table.get("mine"))) {
							roomData.put("myTableId", table.get("tableId"));
							break;
						}
					}
					rooms.add(roomData);
				}

				Map<String, Object> result = new HashMap<>();
				result.put("code", 0);
				result.put("msg", "success");
				result.put("rooms", rooms);
				return ResponseEntity.ok(result);
			}

			return ResponseEntity.ok(errorResponse(500, "获取房间列表失败"));
		} catch (Exception e) {
			logger.error("获取房间列表异常, sessionId: {}", sessionId, e);
			return ResponseEntity.ok(errorResponse(500, "获取房间列表异常: " + e.getMessage()));
		}
	}

	/**
	 * 加入桌子
	 * POST /api/rooms/join { "sessionId": "xxx", "roomId": 1 }
	 */
	@PostMapping("/rooms/join")
	public ResponseEntity<Map<String, Object>> joinTable(@RequestBody Map<String, Object> request) {
		String sessionId = (String) request.get("sessionId");
		Number roomIdNum = (Number) request.get("roomId");

		if (sessionId == null || roomIdNum == null) {
			return ResponseEntity.ok(errorResponse(400, "参数不完整"));
		}

		UserService.UserInfo user = userService.getSession(sessionId);
		if (user == null) {
			return ResponseEntity.ok(errorResponse(401, "会话无效，请重新登录"));
		}

		int roomId = roomIdNum.intValue();
		logger.info("用户请求加入桌子, userId: {}, roomId: {}", user.getUserId(), roomId);

		long startMs = System.currentTimeMillis();
		try {
			CompletableFuture<Message> future = userService.joinTable(sessionId, roomId);
			Message response = future.get(10, TimeUnit.SECONDS);

			if (response instanceof LobbyProto.AckJoinRoomTable) {
				LobbyProto.AckJoinRoomTable ack = (LobbyProto.AckJoinRoomTable) response;

				Map<String, Object> result = new HashMap<>();
				result.put("code", 0);
				result.put("msg", "success");
				result.put("tableId", ack.getTableId());
				return ResponseEntity.ok(result);
			}

			return ResponseEntity.ok(errorResponse(500, "加入桌子失败"));
		} catch (TimeoutException e) {
			logger.error("加入桌子超时, userId: {}, roomId: {}, sessionId: {}, timeoutSec: 10, costMs: {}",
					user.getUserId(), roomId, sessionId, System.currentTimeMillis() - startMs);
			return ResponseEntity.ok(errorResponse(500, "加入桌子超时"));
		} catch (Exception e) {
			logger.error("加入桌子异常, userId: {}, roomId: {}, sessionId: {}, costMs: {}, cause: {}",
					user.getUserId(), roomId, sessionId, System.currentTimeMillis() - startMs, e.toString(), e);
			return ResponseEntity.ok(errorResponse(500, "加入桌子异常: " + e.getMessage()));
		}
	}

	/**
	 * 创建房间：固定模板 或 自定义规则
	 * POST /api/rooms/create
	 * fixed:  { sessionId, mode:"fixed", roomId }
	 * custom: { sessionId, mode:"custom", gameType, baseScore?, totalRounds?, allowChi?, ... }
	 */
	@PostMapping("/rooms/create")
	public ResponseEntity<Map<String, Object>> createRoom(@RequestBody Map<String, Object> request) {
		String sessionId = str(request.get("sessionId"));
		UserService.UserInfo user = userService.getSession(sessionId);
		if (user == null) {
			return ResponseEntity.ok(errorResponse(401, "会话无效，请重新登录"));
		}
		String mode = str(request.get("mode"));
		if (mode.isEmpty()) mode = "fixed";

		try {
			int roomId;
			int gameType;
			if ("custom".equalsIgnoreCase(mode)) {
				Number gt = (Number) request.get("gameType");
				if (gt == null) {
					return ResponseEntity.ok(errorResponse(400, "自定义创房需要 gameType"));
				}
				gameType = gt.intValue();
				Map<String, Object> payload = new HashMap<>();
				payload.put("gameType", gameType);
				copyInt(request, payload, "seatNum");
				copyInt(request, payload, "baseScore");
				copyInt(request, payload, "totalRounds");
				copyInt(request, payload, "maxFan");
				copyInt(request, payload, "allowChi");
				copyInt(request, payload, "allowDianPao");
				copyInt(request, payload, "allowPeng");
				copyInt(request, payload, "allowGang");
				copyInt(request, payload, "allowHu");
				copyInt(request, payload, "autoPlay");
				copyInt(request, payload, "cardNum");
				copyInt(request, payload, "exCardNum");
				Map<String, Object> prepared = lobbyAdminClient.createCustomRoom(user.getToken(), payload);
				if (prepared == null || !Integer.valueOf(0).equals(asInt(prepared.get("code")))) {
					return ResponseEntity.ok(prepared != null ? prepared : errorResponse(502, "lobby 不可用"));
				}
				roomId = asInt(prepared.get("roomId"));
			} else {
				Number roomIdNum = (Number) request.get("roomId");
				if (roomIdNum == null) {
					return ResponseEntity.ok(errorResponse(400, "固定模板创房需要 roomId"));
				}
				roomId = roomIdNum.intValue();
				Number gt = (Number) request.get("gameType");
				gameType = gt != null ? gt.intValue() : 0;
			}

			CompletableFuture<Message> future = userService.joinTable(sessionId, roomId);
			Message response = future.get(10, TimeUnit.SECONDS);
			if (!(response instanceof LobbyProto.AckJoinRoomTable)) {
				return ResponseEntity.ok(errorResponse(500, "创建/加入失败"));
			}
			LobbyProto.AckJoinRoomTable ack = (LobbyProto.AckJoinRoomTable) response;
			Map<String, Object> result = new HashMap<>();
			result.put("code", 0);
			result.put("msg", "success");
			result.put("tableId", ack.getTableId());
			result.put("roomId", roomId);
			result.put("gameType", gameType);
			result.put("mode", mode);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			logger.error("创建房间异常, userId: {}", user.getUserId(), e);
			return ResponseEntity.ok(errorResponse(500, "创建房间异常: " + e.getMessage()));
		}
	}

	private static void copyInt(Map<String, Object> from, Map<String, Object> to, String key) {
		Object v = from.get(key);
		if (v instanceof Number) {
			to.put(key, ((Number) v).intValue());
		}
	}

	private static int asInt(Object o) {
		if (o instanceof Number) return ((Number) o).intValue();
		if (o == null) return 0;
		try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private Map<String, Object> errorResponse(int code, String msg) {
		Map<String, Object> result = new HashMap<>();
		result.put("code", code);
		result.put("msg", msg);
		return result;
	}
}
