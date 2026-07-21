package lobby.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.metrics.MetricsCollector;

/**
 * 统一用户管理器（会话 + 桌子归属）
 */
public class UserManager {
	private static final Logger logger = LoggerFactory.getLogger(UserManager.class);
	private static final UserManager instance = new UserManager();

	private static final int MAX_CAPACITY = 4096;
	private final Map<Long, User> users;

	private UserManager() {
		users = new ConcurrentHashMap<>(MAX_CAPACITY);
		logger.info("用户管理器初始化完成,最大容量: {}", MAX_CAPACITY);
	}

	public static UserManager getInstance() {
		return instance;
	}

	public User getUser(long userId) {
		return users.get(userId);
	}

	public User getUser(int userId) {
		return users.get((long) userId);
	}

	public void removeUser(long userId) {
		User removed = users.remove(userId);
		if (removed != null) {
			removed.destroy();
			logger.info("移除用户, userId: {}", userId);
			MetricsCollector.getInstance().setGauge("lobby.online_users", users.size());
		}
	}

	/**
	 * 添加或替换在线用户（同 userId 更新会话）
	 */
	public User putOrUpdate(User user) {
		if (user == null) {
			return null;
		}
		User existing = users.put(user.getUserId(), user);
		if (existing != null) {
			logger.info("更新在线用户会话, userId: {}", user.getUserId());
		} else {
			logger.debug("添加在线用户, userId: {}", user.getUserId());
			MetricsCollector.getInstance().incrementCounter("lobby.login_total");
		}
		MetricsCollector.getInstance().setGauge("lobby.online_users", users.size());
		return user;
	}

	public boolean addUser(User user) {
		if (user == null) {
			return false;
		}
		User existing = users.putIfAbsent(user.getUserId(), user);
		if (existing != null) {
			logger.warn("用户已在线, userId: {}", user.getUserId());
			return false;
		}
		MetricsCollector.getInstance().incrementCounter("lobby.login_total");
		MetricsCollector.getInstance().setGauge("lobby.online_users", users.size());
		return true;
	}

	public int getUserCount() {
		return users.size();
	}
}
