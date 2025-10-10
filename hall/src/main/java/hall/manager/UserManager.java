package hall.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import hall.db.service.UserService;


/**
 * 玩家管理
 */
public class UserManager {

	private static final UserManager userManager = new UserManager();
	private static final int MAX_SIZE = 4096;
	private final Map<String, User> users;
	private final UserService service = new UserService();

	public UserManager() {
		users = new ConcurrentHashMap<>(MAX_SIZE);
	}

	public static UserManager getInstance() {
		return userManager;
	}

	public User getUser(String cert) {
		return users.get(cert);
	}

	public void removeUser(String userId) {
		User remove = users.remove(userId);
	}

	public void addUser(User user) {
		users.put(user.getCert(), user);
	}
}
