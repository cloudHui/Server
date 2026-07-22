package web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import web.service.UserService;

/**
 * 兼容旧路径；推荐使用 /api/auth/*
 */
@RestController
@RequestMapping("/api")
public class UserController {
	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
		String username = request.get("username");
		String password = request.get("password");
		if (username == null || username.trim().isEmpty()) {
			Map<String, Object> error = new HashMap<>();
			error.put("code", 400);
			error.put("msg", "请使用 username/password 登录，或改用 /api/auth/login");
			return ResponseEntity.badRequest().body(error);
		}
		if (password == null || password.isEmpty()) {
			Map<String, Object> error = new HashMap<>();
			error.put("code", 400);
			error.put("msg", "密码不能为空");
			return ResponseEntity.badRequest().body(error);
		}

		UserService.UserInfo userInfo = userService.login(username.trim(), password);
		if (userInfo == null) {
			Map<String, Object> error = new HashMap<>();
			error.put("code", 401);
			error.put("msg", "登录失败");
			return ResponseEntity.status(401).body(error);
		}
		return ResponseEntity.ok(toSuccess(userInfo));
	}

	@GetMapping("/validate")
	public ResponseEntity<Map<String, Object>> validate(
			@RequestParam(value = "token", required = false) String tokenParam,
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		String token = null;
		if (tokenParam != null && !tokenParam.trim().isEmpty()) {
			token = tokenParam.trim();
		} else if (authorization != null && authorization.startsWith("Bearer ")) {
			token = authorization.substring(7).trim();
		}
		Map<String, Object> result = new HashMap<>();
		if (token == null || token.isEmpty()) {
			result.put("code", 401);
			result.put("msg", "缺少token");
			return ResponseEntity.status(401).body(result);
		}
		UserService.UserInfo userInfo = userService.validateToken(token);
		if (userInfo != null) {
			return ResponseEntity.ok(toSuccess(userInfo));
		}
		result.put("code", 401);
		result.put("msg", "Token无效或已过期");
		return ResponseEntity.ok(result);
	}

	@PostMapping("/logout")
	public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, String> request) {
		String sessionId = request.get("sessionId");
		if (sessionId != null) {
			userService.logout(sessionId);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("code", 0);
		result.put("msg", "success");
		ResponseCookie cookie = ResponseCookie.from("sessionId", "")
				.path("/").maxAge(0).httpOnly(true).sameSite("Lax").build();
		return ResponseEntity.ok().header("Set-Cookie", cookie.toString()).body(result);
	}

	private Map<String, Object> toSuccess(UserService.UserInfo userInfo) {
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
}
