package hall.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserMap {

	private final Map<Integer, User> users;

	public UserMap() {
		this.users = new ConcurrentHashMap<>();
	}

	private Map<Integer, User> getUsers() {
		return users;
	}

	public User getUser(int userId) {
		return getUsers().get(userId);
	}

	public void addUser(User user) {
		getUsers().put(user.getUserId(), user);
	}
}
