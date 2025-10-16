package hall.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		logger.info("用户管理器初始化完成，最大容量: {}", MAX_CAPACITY);
	}

	public static UserManager getInstance() {
		return instance;
	}

	/**
	 * 根据用户ID获取用户
	 */
	public User getUser(String cert) {
		User user = usersC.get(cert);
		if (user == null) {
			logger.debug("用户不存在, cert: {}", cert);
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
		User removedUser = users.remove(userId);
		if (removedUser != null) {
			logger.info("移除用户, userId: {}", userId);
			String cert = removedUser.getCert();
			removedUser = usersC.remove(cert);
			if (removedUser != null) {
				logger.info("移除用户, cert: {}", cert);
			} else {
				logger.warn("用户不存在，无法移除, cert: {}", cert);
			}
		} else {
			logger.warn("用户不存在，无法移除, userId: {}", userId);
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
		User existingUser = users.putIfAbsent(userId, user);

		if (existingUser != null) {
			logger.warn("用户已存在，添加失败, userId: {}", userId);
		} else {
			logger.debug("添加新用户, userId: {}", userId);
		}

		existingUser = usersC.putIfAbsent(user.getCert(), user);

		if (existingUser != null) {
			logger.warn("用户已存在，添加失败, userId: {}", userId);
		} else {
			logger.debug("添加新用户, userId: {}", userId);
		}
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