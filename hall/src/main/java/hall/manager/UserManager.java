package hall.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import db.service.UserService;

/**
 * 玩家管理
 */
public class UserManager {

	private static final UserManager userManager = new UserManager();

	public static UserManager getInstance() {
		return userManager;
	}

	private final Map<Integer, UserMap> users;

	private final UserService service = new UserService();

	public UserManager() {
		this.users = new ConcurrentHashMap<>();
	}



	public HallUser getUser(int userId) {
		int index = userId / 16;
		UserMap userMap = users.computeIfAbsent(index, k -> new UserMap());
		return userMap.getUser(userId);
	}

	public void addUser(HallUser user) {
		int userId = user.getUserId();
		int index = userId / 16;
		UserMap userMap = users.computeIfAbsent(index, k -> new UserMap());
		userMap.addUser(user);
	}
}
