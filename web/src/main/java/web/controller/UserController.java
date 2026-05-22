package web.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import web.service.UserService;

/**
 * 用户登录和认证接口
 */
@RestController
@RequestMapping("/api")
public class UserController {
	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * 登录接口
	 * POST /api/login { "nickname": "玩家名" }
	 */
	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
		String nickname = request.get("nickname");
		if (nickname == null || nickname.trim().isEmpty()) {
			Map<String, Object> error = new HashMap<>();
			error.put("code", 400);
			error.put("msg", "昵称不能为空");
			return ResponseEntity.badRequest().body(error);
		}

		UserService.UserInfo userInfo = userService.login(nickname.trim());
		if (userInfo == null) {
			Map<String, Object> error = new HashMap<>();
			error.put("code", 500);
			error.put("msg", "登录失败，请重试");
			return ResponseEntity.status(500).body(error);
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

	/**
	 * Token验证接口
	 * GET /api/validate?token=xxx
	 */
	@GetMapping("/validate")
	public ResponseEntity<Map<String, Object>> validate(@RequestParam String token) {
		UserService.UserInfo userInfo = userService.validateToken(token);

		Map<String, Object> result = new HashMap<>();
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

	/**
	 * 登出接口
	 * POST /api/logout { "sessionId": "xxx" }
	 */
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
