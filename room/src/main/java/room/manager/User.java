package room.manager;

import proto.ServerProto;

public class User {

	private final int userId;

	private ServerProto.RoomRole role;

	public User(int userId) {
		this.userId = userId;
	}

	public int getUserId() {
		return userId;
	}

	public void destroy() {

	}
}