package hall.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户信息模型
 * 代表一个登录用户的基本信息和会话状态
 */
public class User {
	private static final Logger logger = LoggerFactory.getLogger(User.class);

	private final int userId;
	private final String cert;
	private String nick;
	private int clientId;
	private long lastActiveTime;

	public User(int userId, String nick, int clientId, String cert) {
		this.userId = userId;
		this.nick = nick;
		this.clientId = clientId;
		this.cert = cert;
		this.lastActiveTime = System.currentTimeMillis();

		logger.debug("创建用户实例, userId: {}, nick: {}, clientId: {}", userId, nick, clientId);
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
		logger.debug("更新用户昵称, userId: {}, newNick: {}", userId, nick);
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
		updateActiveTime();
		logger.debug("更新用户客户端ID, userId: {}, newClientId: {}", userId, clientId);
	}

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	/**
	 * 更新最后活跃时间
	 */
	public void updateActiveTime() {
		this.lastActiveTime = System.currentTimeMillis();
	}

	/**
	 * 检查用户是否活跃（在指定时间内有活动）
	 */
	public boolean isActive(long timeoutMillis) {
		return (System.currentTimeMillis() - lastActiveTime) < timeoutMillis;
	}

	@Override
	public String toString() {
		return String.format("User{userId=%d, nick='%s', clientId=%d, cert='%s'}",
				userId, nick, clientId, cert);
	}
}