package web.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import web.service.ReplayService;
import web.service.UserService;

/** 普通玩家回放接口，只返回本人参与的回放。 */
@RestController
@RequestMapping("/api/replays")
public class ReplayController {
	private final ReplayService replayService;
	private final UserService userService;

	public ReplayController(ReplayService replayService, UserService userService) {
		this.replayService = replayService;
		this.userService = userService;
	}

	@GetMapping
	public ResponseEntity<Map<String, Object>> list(@RequestParam String sessionId,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
		UserService.UserInfo user = userService.getSession(sessionId);
		if (user == null) return ResponseEntity.ok(error(401, "会话无效"));
		Map<String, Object> result = replayService.pageForUser(user.getUserId(), page, size);
		result.put("code", 0);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/code")
	public ResponseEntity<Map<String, Object>> byCode(@RequestParam String sessionId, @RequestParam String code) {
		UserService.UserInfo user = userService.getSession(sessionId);
		if (user == null) return ResponseEntity.ok(error(401, "会话无效"));
		int slash = code == null ? -1 : code.indexOf('/');
		if (slash <= 0 || slash == code.length() - 1) return ResponseEntity.ok(error(400, "回放码格式为 日期/文件名"));
		return ResponseEntity.ok(replayService.getReplayForUser(user.getUserId(), code.substring(0, slash), code.substring(slash + 1)));
	}

	private static Map<String, Object> error(int code, String msg) {
		Map<String, Object> result = new java.util.HashMap<>(); result.put("code", code); result.put("msg", msg); return result;
	}
}
