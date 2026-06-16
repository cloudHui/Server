package hall.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户信息模型
 * 代表一个登录用户的基本信息和会话状态
 */
public class User {
	private static final Logger logger = LoggerFactory.getLogger(User.class);

	private final long userId;
	private String deviceId;
	private String nick;
	private int gateId;
	private long lastActiveTime;
	private String pendingToken;

	public User(long userId, String nick, int gateId, String deviceId) {
		this.userId = userId;
		this.nick = nick;
		this.gateId = gateId;
		this.deviceId = deviceId;
		this.lastActiveTime = System.currentTimeMillis();

		logger.debug("创建用户实例, userId: {}, nick: {}, gateId: {}", userId, nick, gateId);
	}

	public long getUserId() {
		return userId;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
		logger.debug("更新用户昵称, userId: {}, newNick: {}", userId, nick);
	}

	public int getGateId() {
		return gateId;
	}

	public void setGateId(int gateId) {
		this.gateId = gateId;
		updateActiveTime();
		logger.debug("更新用户网关ID, userId: {}, newGateId: {}", userId, gateId);
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

	public String getPendingToken() {
		return pendingToken;
	}

	public void setPendingToken(String pendingToken) {
		this.pendingToken = pendingToken;
	}

	/**
	 * 获取并清除pendingToken（一次性消费）
	 */
	public synchronized String consumePendingToken() {
		String token = this.pendingToken;
		this.pendingToken = null;
		return token;
	}

	@Override
	public String toString() {
		return String.format("User{userId=%d, nick='%s', gateId=%d, deviceId='%s'}",
				userId, nick, gateId, deviceId);
	}
}