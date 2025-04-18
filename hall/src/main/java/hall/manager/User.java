package hall.manager;

public class User {

	private int userId;

	private String nick;

	private long clientId;

	public User() {
	}

	public User(int userId, String nick, long clientId) {
		this.userId = userId;
		this.nick = nick;
		this.clientId = clientId;
	}

	public int getUserId() {
		return userId;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public long getClientId() {
		return clientId;
	}
}