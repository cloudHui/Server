package web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proto.ModelProto;
import proto.RoomProto;
import web.service.UserService;

/**
 * 房间和桌子管理接口
 */
@RestController
@RequestMapping("/api")
public class RoomController {
	private static final Logger logger = LoggerFactory.getLogger(RoomController.class);

	private final UserService userService;

	public RoomController(UserService userService) {
		this.userService = userService;
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

			if (response instanceof RoomProto.AckRoomList) {
				RoomProto.AckRoomList ackRoomList = (RoomProto.AckRoomList) response;
				List<Map<String, Object>> rooms = new ArrayList<>();

				for (ModelProto.Room room : ackRoomList.getRoomListList()) {
					Map<String, Object> roomData = new HashMap<>();
					roomData.put("roomId", room.getRoomId());

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
						tables.add(tableData);
					}
					roomData.put("tables", tables);
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

		try {
			CompletableFuture<Message> future = userService.joinTable(sessionId, roomId);
			Message response = future.get(10, TimeUnit.SECONDS);

			if (response instanceof RoomProto.AckJoinRoomTable) {
				RoomProto.AckJoinRoomTable ack = (RoomProto.AckJoinRoomTable) response;

				Map<String, Object> result = new HashMap<>();
				result.put("code", 0);
				result.put("msg", "success");
				result.put("tableId", ack.getTableId());
				return ResponseEntity.ok(result);
			}

			return ResponseEntity.ok(errorResponse(500, "加入桌子失败"));
		} catch (Exception e) {
			logger.error("加入桌子异常, userId: {}, roomId: {}", user.getUserId(), roomId, e);
			return ResponseEntity.ok(errorResponse(500, "加入桌子异常: " + e.getMessage()));
		}
	}

	private Map<String, Object> errorResponse(int code, String msg) {
		Map<String, Object> result = new HashMap<>();
		result.put("code", code);
		result.put("msg", msg);
		return result;
	}
}
