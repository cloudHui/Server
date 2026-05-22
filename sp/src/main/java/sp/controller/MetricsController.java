package sp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 指标端点控制器
 * 提供简单的JSON格式运行状态数据
 */
@RestController
public class MetricsController {

	@GetMapping("/metrics")
	public Map<String, Object> metrics() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("service", "sp");
		result.put("timestamp", System.currentTimeMillis());

		Runtime runtime = Runtime.getRuntime();
		Map<String, Object> jvm = new LinkedHashMap<>();
		jvm.put("maxMemory", runtime.maxMemory());
		jvm.put("totalMemory", runtime.totalMemory());
		jvm.put("freeMemory", runtime.freeMemory());
		jvm.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
		jvm.put("availableProcessors", runtime.availableProcessors());
		result.put("jvm", jvm);

		return result;
	}

	@GetMapping("/health")
	public Map<String, String> health() {
		Map<String, String> result = new LinkedHashMap<>();
		result.put("status", "UP");
		result.put("service", "sp");
		return result;
	}
}
