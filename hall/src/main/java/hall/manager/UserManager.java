package hall.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家管理
 */
public class UserManager {

	private static UserManager userManager = new UserManager();

	public static UserManager getInstance() {
		return userManager;
	}

	private Map<Integer, UserMap> users;

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
