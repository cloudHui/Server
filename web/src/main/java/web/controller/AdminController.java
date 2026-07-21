package web.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import web.service.ReplayService;
import web.service.UserService;

/**
 * 管理后台 API：邀请 / 玩家 / 桌子 / 回放
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
	private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

	private final UserService userService;
	private final LobbyAdminClient lobbyAdminClient;
	private final ReplayService replayService;

	public AdminController(UserService userService, LobbyAdminClient lobbyAdminClient, ReplayService replayService) {
		this.userService = userService;
		this.lobbyAdminClient = lobbyAdminClient;
		this.replayService = replayService;
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

	@GetMapping("/users")
	public ResponseEntity<Map<String, Object>> users(@RequestParam String sessionId) {
		UserService.UserInfo user = requireAdmin(sessionId);
		if (user == null) {
			return ResponseEntity.ok(error(403, "需要管理员账号"));
		}
		Map<String, Object> result = lobbyAdminClient.listUsers(user.getToken());
		return ResponseEntity.ok(result != null ? result : error(502, "lobby admin 不可用"));
	}

	@PostMapping("/users/enable")
	public ResponseEntity<Map<String, Object>> enableUser(@RequestBody Map<String, Object> body) {
		String sessionId = str(body.get("sessionId"));
		UserService.UserInfo user = requireAdmin(sessionId);
		if (user == null) {
			return ResponseEntity.ok(error(403, "需要管理员账号"));
		}
		Map<String, Object> payload = new HashMap<>();
		payload.put("userId", body.get("userId"));
		payload.put("enabled", body.get("enabled"));
		Map<String, Object> result = lobbyAdminClient.enableUser(user.getToken(), payload);
		return ResponseEntity.ok(result != null ? result : error(502, "lobby admin 不可用"));
	}

	@GetMapping("/tables")
	public ResponseEntity<Map<String, Object>> tables(@RequestParam String sessionId) {
		UserService.UserInfo user = requireAdmin(sessionId);
		if (user == null) {
			return ResponseEntity.ok(error(403, "需要管理员账号"));
		}
		Map<String, Object> result = lobbyAdminClient.listTables(user.getToken());
		return ResponseEntity.ok(result != null ? result : error(502, "lobby admin 不可用"));
	}

	@GetMapping("/replays")
	public ResponseEntity<Map<String, Object>> replays(@RequestParam String sessionId,
			@RequestParam(required = false, defaultValue = "1") int page,
			@RequestParam(required = false, defaultValue = "20") int size) {
		UserService.UserInfo user = requireAdmin(sessionId);
		if (user == null) {
			return ResponseEntity.ok(error(403, "需要管理员账号"));
		}
		Map<String, Object> result = replayService.page(page, size);
		result.put("code", 0);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/records")
	public ResponseEntity<Map<String, Object>> records(@RequestParam String sessionId,
			@RequestParam(required = false, defaultValue = "1") int page,
			@RequestParam(required = false, defaultValue = "20") int size) {
		UserService.UserInfo user = requireAdmin(sessionId);
		if (user == null) return ResponseEntity.ok(error(403, "需要管理员账号"));
		Map<String, Object> result = new HashMap<>();
		result.put("code", 0);
		result.put("page", page);
		result.put("size", size);
		result.put("records", lobbyAdminClient.listRecords(user.getToken(), page, size));
		return ResponseEntity.ok(result);
	}

	@GetMapping("/replays/detail")
	public ResponseEntity<Map<String, Object>> replayDetail(@RequestParam String sessionId,
			@RequestParam String date, @RequestParam String name) {
		UserService.UserInfo user = requireAdmin(sessionId);
		if (user == null) {
			return ResponseEntity.ok(error(403, "需要管理员账号"));
		}
		return ResponseEntity.ok(replayService.getReplay(date, name));
	}

	@GetMapping("/replays/code")
	public ResponseEntity<Map<String, Object>> replayByCode(@RequestParam String sessionId,
			@RequestParam String code) {
		UserService.UserInfo user = requireAdmin(sessionId);
		if (user == null) return ResponseEntity.ok(error(403, "需要管理员账号"));
		int slash = code == null ? -1 : code.indexOf('/');
		if (slash <= 0 || slash == code.length() - 1) return ResponseEntity.ok(error(400, "回放码格式为 日期/文件名"));
		return ResponseEntity.ok(replayService.getReplay(code.substring(0, slash), code.substring(slash + 1)));
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
