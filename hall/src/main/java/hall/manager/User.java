package hall.manager;

public class User {

	private final int userId;

	private String nick;

	private int clientId;

	private final String cert;

	public User(int userId, String nick, int clientId, String cert) {
		this.userId = userId;
		this.nick = nick;
		this.clientId = clientId;
		this.cert = cert;
	}

	public String getCert() {
		return cert;
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