package game.manager.model;

import model.User;

public class GameUser extends User {

	public GameUser(int userId) {
		setOnLine(true);
		setSit(true);
		setUserId(userId);
	}

	@Override
	public String toString() {
		return super.toString() + "User{" +
				'}';
	}
}
