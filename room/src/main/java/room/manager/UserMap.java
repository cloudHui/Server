package room.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserMap {

	private Map<Integer, RoomUser> users;

	public UserMap() {
		this.users = new ConcurrentHashMap<>();
	}

	private Map<Integer, RoomUser> getUsers() {
		return users;
	}

	public RoomUser getUser(int userId) {
		return getUsers().get(userId);
	}

	public void addUser(RoomUser user) {
		getUsers().put(user.getUserId(), user);
	}
}
