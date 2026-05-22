package hall.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Token管理器
 * 负责Token的生成、校验和过期清理
 * Token有效期：14天不活跃过期
 */
public class TokenManager {
	private static final Logger logger = LoggerFactory.getLogger(TokenManager.class);
	private static final TokenManager instance = new TokenManager();

	/** 14天毫秒数 */
	private static final long TOKEN_EXPIRY_MILLIS = 14L * 24 * 60 * 60 * 1000;

	/** token -> userId */
	private final Map<String, Integer> tokenToUser = new ConcurrentHashMap<>();

	/** token -> 最后活跃时间 */
	private final Map<String, Long> tokenActiveTime = new ConcurrentHashMap<>();

	/** userId -> token (确保每个用户只有一个有效token) */
	private final Map<Integer, String> userToToken = new ConcurrentHashMap<>();

	private TokenManager() {}

	public static TokenManager getInstance() {
		return instance;
	}

	/**
	 * 生成Token并绑定用户
	 */
	public String generateToken(int userId) {
		// 旧token失效
		String oldToken = userToToken.remove(userId);
		if (oldToken != null) {
			tokenToUser.remove(oldToken);
			tokenActiveTime.remove(oldToken);
		}

		String token = UUID.randomUUID().toString().replace("-", "");
		tokenToUser.put(token, userId);
		tokenActiveTime.put(token, System.currentTimeMillis());
		userToToken.put(userId, token);

		logger.info("生成Token, userId: {}, token: {}", userId, token);
		return token;
	}

	/**
	 * 根据Token获取用户ID，过期则返回0
	 */
	public int getUserIdByToken(String token) {
		if (token == null || token.isEmpty()) {
			return 0;
		}
		Integer userId = tokenToUser.get(token);
		if (userId == null) {
			return 0;
		}
		// 检查过期
		Long lastActive = tokenActiveTime.get(token);
		if (lastActive == null || System.currentTimeMillis() - lastActive > TOKEN_EXPIRY_MILLIS) {
			invalidateToken(token);
			logger.info("Token已过期, token: {}", token);
			return 0;
		}
		return userId;
	}

	/**
	 * 刷新Token活跃时间
	 */
	public void refreshToken(String token) {
		if (token != null && tokenToUser.containsKey(token)) {
			tokenActiveTime.put(token, System.currentTimeMillis());
		}
	}

	/**
	 * 使Token失效
	 */
	public void invalidateToken(String token) {
		if (token == null) return;
		Integer userId = tokenToUser.remove(token);
		tokenActiveTime.remove(token);
		if (userId != null) {
			userToToken.remove(userId);
		}
	}

	/**
	 * 使用户的所有Token失效
	 */
	public void invalidateUser(int userId) {
		String token = userToToken.remove(userId);
		if (token != null) {
			tokenToUser.remove(token);
			tokenActiveTime.remove(token);
		}
	}

	/**
	 * 清理过期Token
	 */
	public void cleanupExpired() {
		long now = System.currentTimeMillis();
		tokenActiveTime.entrySet().removeIf(entry -> {
			if (now - entry.getValue() > TOKEN_EXPIRY_MILLIS) {
				String token = entry.getKey();
				Integer userId = tokenToUser.remove(token);
				if (userId != null) {
					userToToken.remove(userId);
				}
				return true;
			}
			return false;
		});
	}
}
