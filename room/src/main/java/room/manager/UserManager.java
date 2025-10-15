package room.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 玩家管理
 */
public class UserManager {

	private static final UserManager userManager = new UserManager();
	private static final int MAX_SIZE = 4096;
	private final Map<Integer, User> userIds;

	public UserManager() {
		userIds = new ConcurrentHashMap<>(MAX_SIZE);
	}

	public static UserManager getInstance() {
		return userManager;
	}

	public User getUser(int id) {
		return userIds.get(id);
	}

	public void removeUser(int userId) {
		User remove = userIds.remove(userId);
		remove.destroy();
	}

	public void addUser(User user) {
		userIds.put(user.getUserId(), user);
	}
}
