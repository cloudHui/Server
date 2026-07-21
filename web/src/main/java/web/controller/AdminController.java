package web.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import web.service.UserService;

/**
 * 邀请管理 API（反代 lobby admin HTTP；仅 admin）
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
	private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

	private final UserService userService;
	private final LobbyAdminClient lobbyAdminClient;

	public AdminController(UserService userService, LobbyAdminClient lobbyAdminClient) {
		this.userService = userService;
		this.lobbyAdminClient = lobbyAdminClient;
	}

	@GetMapping("/invites")
	public ResponseEntity<Map<String, Object>> list(@RequestParam String sessionId) {
		UserService.UserInfo user = requireAdmin(sessionId);
		if (user == null) {
			return ResponseEntity.ok(error(403, "需要管理员账号"));
		}
		Map<String, Object> result = lobbyAdminClient.listInvites(user.getToken());
		return ResponseEntity.ok(result != null ? result : error(502, "lobby admin 不可用"));
	}

	@PostMapping("/invites")
	public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
		String sessionId = str(body.get("sessionId"));
		UserService.UserInfo user = requireAdmin(sessionId);
		if (user == null) {
			return ResponseEntity.ok(error(403, "需要管理员账号"));
		}
		Map<String, Object> payload = new HashMap<>();
		payload.put("note", str(body.get("note")));
		payload.put("maxUses", body.get("maxUses") == null ? 1 : body.get("maxUses"));
		payload.put("expiresDays", body.get("expiresDays") == null ? 7 : body.get("expiresDays"));
		Map<String, Object> result = lobbyAdminClient.createInvite(user.getToken(), payload);
		return ResponseEntity.ok(result != null ? result : error(502, "lobby admin 不可用"));
	}

	@PostMapping("/invites/revoke")
	public ResponseEntity<Map<String, Object>> revoke(@RequestBody Map<String, Object> body) {
		String sessionId = str(body.get("sessionId"));
		UserService.UserInfo user = requireAdmin(sessionId);
		if (user == null) {
			return ResponseEntity.ok(error(403, "需要管理员账号"));
		}
		Map<String, Object> payload = new HashMap<>();
		payload.put("token", str(body.get("token")));
		Map<String, Object> result = lobbyAdminClient.revokeInvite(user.getToken(), payload);
		return ResponseEntity.ok(result != null ? result : error(502, "lobby admin 不可用"));
	}

	private UserService.UserInfo requireAdmin(String sessionId) {
		if (sessionId == null || sessionId.isEmpty()) {
			return null;
		}
		UserService.UserInfo user = userService.getSession(sessionId);
		if (user == null || !user.isAdmin()) {
			return null;
		}
		return user;
	}

	private static Map<String, Object> error(int code, String msg) {
		Map<String, Object> result = new HashMap<>();
		result.put("code", code);
		result.put("msg", msg);
		return result;
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
