package hall.manager;

public class User {

	private final int userId;

	private String nick;

	private int clientId;

	public User(int userId, String nick, int clientId) {
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

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}
}