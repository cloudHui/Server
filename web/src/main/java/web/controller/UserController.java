package web.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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
			// 兼容旧 nickname 字段：不再支持游客开号
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

		Map<String, Object> result = new HashMap<>();
		result.put("code", 0);
		result.put("msg", "success");
		result.put("sessionId", userInfo.getSessionId());
		result.put("userId", userInfo.getUserId());
		result.put("nickname", userInfo.getNickname());
		result.put("token", userInfo.getToken());
		return ResponseEntity.ok(result);
	}

	@GetMapping("/validate")
	public ResponseEntity<Map<String, Object>> validate(
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		Map<String, Object> result = new HashMap<>();
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			result.put("code", 401);
			result.put("msg", "缺少Authorization头");
			return ResponseEntity.status(401).body(result);
		}
		String token = authorization.substring(7).trim();
		UserService.UserInfo userInfo = userService.validateToken(token);
		if (userInfo != null) {
			result.put("code", 0);
			result.put("msg", "success");
			result.put("sessionId", userInfo.getSessionId());
			result.put("userId", userInfo.getUserId());
			result.put("nickname", userInfo.getNickname());
			result.put("token", userInfo.getToken());
		} else {
			result.put("code", 401);
			result.put("msg", "Token无效或已过期");
		}
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
		return ResponseEntity.ok(result);
	}
}
