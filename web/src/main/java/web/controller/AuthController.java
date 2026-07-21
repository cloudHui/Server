package web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import web.service.UserService;

/**
 * 认证接口（对齐 /api/auth/*）
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final UserService userService;
	private final LobbyAdminClient lobbyAdminClient;

	public AuthController(UserService userService, LobbyAdminClient lobbyAdminClient) {
		this.userService = userService;
		this.lobbyAdminClient = lobbyAdminClient;
	}

	/**
	 * POST /api/auth/login { "username","password" } 或 { "token" }
	 */
	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
		String token = request.get("token");
		if (token != null && !token.trim().isEmpty()) {
			UserService.UserInfo userInfo = userService.validateToken(token.trim());
			if (userInfo == null) {
				return ResponseEntity.ok(error(401, "Token无效或已过期"));
			}
			return ResponseEntity.ok(success(userInfo));
		}

		String username = request.get("username");
		String password = request.get("password");
		if (username == null || username.trim().isEmpty()
				|| password == null || password.isEmpty()) {
			return ResponseEntity.badRequest().body(error(400, "用户名和密码不能为空"));
		}

		UserService.UserInfo userInfo = userService.login(username.trim(), password);
		if (userInfo == null) {
			return ResponseEntity.ok(error(401, "登录失败，用户名或密码错误"));
		}
		return ResponseEntity.ok(success(userInfo));
	}

	/**
	 * POST /api/auth/register { "username","password","nickname?,"invite?" }
	 */
	@PostMapping("/register")
	public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
		String username = request.get("username");
		String password = request.get("password");
		String nickname = request.get("nickname");
		String invite = request.get("invite");

		if (username == null || username.trim().isEmpty()
				|| password == null || password.isEmpty()) {
			return ResponseEntity.badRequest().body(error(400, "用户名和密码不能为空"));
		}

		UserService.UserInfo userInfo = userService.register(
				username.trim(), password,
				nickname == null ? username.trim() : nickname.trim(),
				invite);
		if (userInfo == null) {
			return ResponseEntity.ok(error(500, "注册失败"));
		}
		if (userInfo.getUserId() <= 0) {
			return ResponseEntity.ok(error(userInfo.getErrorCode(), registerMsg(userInfo.getErrorCode())));
		}
		return ResponseEntity.ok(success(userInfo));
	}

	/**
	 * GET /api/auth/registration — 注册策略提示
	 */
	@GetMapping("/registration")
	public ResponseEntity<Map<String, Object>> registration() {
		Map<String, Object> fromLobby = lobbyAdminClient.getRegistration();
		if (fromLobby != null && fromLobby.containsKey("openRegister")) {
			return ResponseEntity.ok(fromLobby);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("code", 0);
		result.put("openRegister", false);
		result.put("inviteRequired", true);
		result.put("msg", "默认关闭开放注册，需邀请码");
		return ResponseEntity.ok(result);
	}

	private Map<String, Object> success(UserService.UserInfo userInfo) {
		Map<String, Object> result = new HashMap<>();
		result.put("code", 0);
		result.put("msg", "success");
		result.put("sessionId", userInfo.getSessionId());
		result.put("userId", userInfo.getUserId());
		result.put("username", userInfo.getUsername());
		result.put("nickname", userInfo.getNickname());
		result.put("token", userInfo.getToken());
		result.put("tables", userInfo.getTables());
		List<Map<String, Object>> infos = new ArrayList<>();
		for (UserService.TableInfoView t : userInfo.getTableInfos()) {
			infos.add(t.toMap());
		}
		result.put("tableInfos", infos);
		result.put("isAdmin", userInfo.isAdmin());
		return result;
	}

	private Map<String, Object> error(int code, String msg) {
		Map<String, Object> result = new HashMap<>();
		result.put("code", code);
		result.put("msg", msg);
		return result;
	}

	private String registerMsg(int code) {
		switch (code) {
			case 2: return "用户名已存在";
			case 3: return "需要邀请码";
			case 4: return "邀请码无效";
			default: return "注册失败";
		}
	}
}
