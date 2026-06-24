package gate.rate;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 连接频率限制器
 * 同一设备ID在10秒内最多3次连接，超出静默拒绝
 * 内部定期清理过期条目，防止内存泄漏
 */
public class ConnectionRateLimiter {
	private static final Logger logger = LoggerFactory.getLogger(ConnectionRateLimiter.class);

	/** 时间窗口：10秒 */
	private static final long WINDOW_MILLIS = 10_000;

	/** 窗口内最大连接数 */
	private static final int MAX_CONNECTIONS = 3;

	/** 每N次allow调用触发一次全量清理 */
	private static final int CLEANUP_INTERVAL = 100;

	/** deviceId -> 连接时间戳队列 */
	private final Map<String, Deque<Long>> deviceConnections = new ConcurrentHashMap<>();

	/** allow调用计数器，用于触发定期清理 */
	private int callCount;

	/**
	 * 检查设备是否允许连接
	 * @param deviceId 设备ID
	 * @return true=允许 false=拒绝
	 */
	public boolean allow(String deviceId) {
		if (deviceId == null || deviceId.isEmpty()) {
			return true;
		}

		long now = System.currentTimeMillis();
		Deque<Long> timestamps = deviceConnections.computeIfAbsent(
				deviceId, k -> new ConcurrentLinkedDeque<>());

		// 清理窗口外的时间戳
		while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
			timestamps.pollFirst();
		}

		if (timestamps.size() >= MAX_CONNECTIONS) {
			logger.warn("设备连接频率超限, deviceId: {}, 10秒内连接{}次",
					deviceId, timestamps.size());
			return false;
		}

		timestamps.addLast(now);

		// 定期全量清理过期条目，防止map无限增长
		if (++callCount >= CLEANUP_INTERVAL) {
			callCount = 0;
			cleanupExpired();
		}

		return true;
	}

	/** 清理所有过期的时间戳队列和空条目 */
	private void cleanupExpired() {
		long now = System.currentTimeMillis();
		deviceConnections.entrySet().removeIf(entry -> {
			Deque<Long> timestamps = entry.getValue();
			while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
				timestamps.pollFirst();
			}
			return timestamps.isEmpty();
		});
	}
}
