package hall.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import hall.db.service.UserService;


/**
 * 玩家管理
 */
public class UserManager {

	private static final UserManager userManager = new UserManager();

	public static UserManager getInstance() {
		return userManager;
	}

	private static final int MAX_SIZE = 4096;
	private final Map<Integer, User> users;

	private final Map<Integer, User> clientUser;

	private final UserService service = new UserService();

	public UserManager() {
		users = new ConcurrentHashMap<>(MAX_SIZE);
		clientUser = new ConcurrentHashMap<>(MAX_SIZE);
	}

	public User getUser(int userId) {
		return users.get(userId);
	}

	public void removeUser(int userId) {
		User remove = users.remove(userId);
		clientUser.remove(remove.getClientId());
	}

	public void addUser(User user) {
		users.put(user.getUserId(), user);
		clientUser.put(user.getClientId(), user);
	}
}
