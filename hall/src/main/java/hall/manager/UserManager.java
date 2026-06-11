package hall.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.metrics.MetricsCollector;

/**
 * 用户管理器
 * 负责用户会话的创建、查找和销毁
 */
public class UserManager {
	private static final Logger logger = LoggerFactory.getLogger(UserManager.class);
	private static final UserManager instance = new UserManager();

	private static final int MAX_CAPACITY = 4096;
	private final Map<Integer, User> users;
	private final Map<String, User> usersC;

	private UserManager() {
		users = new ConcurrentHashMap<>(MAX_CAPACITY);
		usersC = new ConcurrentHashMap<>(MAX_CAPACITY);
		logger.info("用户管理器初始化完成,最大容量: {}", MAX_CAPACITY);
	}

	public static UserManager getInstance() {
		return instance;
	}

	/**
	 * 根据设备ID获取用户
	 */
	public User getUser(String deviceId) {
		User user = usersC.get(deviceId);
		if (user == null) {
			logger.debug("用户不存在, deviceId: {}", deviceId);
		}
		return user;
	}

	/**
	 * 根据用户ID获取用户
	 */
	public User getUser(int id) {
		User user = users.get(id);
		if (user == null) {
			logger.debug("用户不存在, id: {}", id);
		}
		return user;
	}

	/**
	 * 移除用户
	 */
	public void removeUser(int userId) {
		synchronized (this) {
			User removedUser = users.remove(userId);
			if (removedUser != null) {
				logger.info("移除用户, userId: {}", userId);
				String deviceId = removedUser.getDeviceId();
				if (deviceId != null) {
					User removedByDevice = usersC.remove(deviceId);
					if (removedByDevice != null) {
						logger.info("移除用户, deviceId: {}", deviceId);
					} else {
						logger.warn("用户不存在于devicesC, deviceId: {}", deviceId);
					}
				} else {
					logger.warn("用户deviceId为空,跳过devicesC移除, userId: {}", userId);
				}
				MetricsCollector.getInstance().setGauge("hall.online_users", users.size());
			} else {
				logger.warn("用户不存在,无法移除, userId: {}", userId);
			}
		}
	}

	/**
	 * 添加用户
	 */
	public void addUser(User user) {
		if (user == null) {
			logger.error("无法添加空用户");
			return;
		}

		int userId = user.getUserId();
		String deviceId = user.getDeviceId();

		synchronized (this) {
			User existingById = users.putIfAbsent(userId, user);
			if (existingById != null) {
				logger.warn("用户已存在,添加失败, userId: {}", userId);
				return;
			}

			User existingByDevice = usersC.putIfAbsent(deviceId, user);
			if (existingByDevice != null) {
				// Roll back: remove the userId entry we just added
				users.remove(userId, user);
				logger.warn("设备已存在,添加失败, deviceId: {}, userId: {}", deviceId, userId);
				return;
			}

			logger.debug("添加新用户, userId: {}, deviceId: {}", userId, deviceId);
			MetricsCollector.getInstance().incrementCounter("hall.login_total");
		}

		MetricsCollector.getInstance().setGauge("hall.online_users", users.size());
	}

	/**
	 * 获取当前用户数量
	 */
	public int getUserCount() {
		return users.size();
	}

	/**
	 * 检查用户是否存在
	 */
	public boolean containsUser(int userId) {
		return users.containsKey(userId);
	}
}