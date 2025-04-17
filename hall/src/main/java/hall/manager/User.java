package hall.manager;

public class User {

	private int userId;

	private String nick;

	public User() {
	}

	public User(int userId, String nick) {
		this.userId = userId;
		this.nick = nick;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}
}