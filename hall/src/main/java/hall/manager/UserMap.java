package hall.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserMap {

	private final Map<Integer, HallUser> users;

	public UserMap() {
		this.users = new ConcurrentHashMap<>();
	}

	private Map<Integer, HallUser> getUsers() {
		return users;
	}

	public HallUser getUser(int userId) {
		return getUsers().get(userId);
	}

	public void addUser(HallUser user) {
		getUsers().put(user.getUserId(), user);
	}
}
