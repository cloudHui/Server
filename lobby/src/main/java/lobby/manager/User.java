package lobby.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

/**
 * Lobby 统一用户：会话（gate）+ 桌子归属
 */
public class User {
	private static final Logger logger = LoggerFactory.getLogger(User.class);

	private final long userId;
	private String username;
	private String nick;
	private int gateId;
	private long lastActiveTime;
	private String pendingToken;
	private boolean joinGame;
	private boolean offline;
	private final Set<Long> tables = ConcurrentHashMap.newKeySet();

	public User(long userId, String username, String nick, int gateId) {
		this.userId = userId;
		this.username = username;
		this.nick = nick;
		this.gateId = gateId;
		this.lastActiveTime = System.currentTimeMillis();
		logger.debug("创建用户, userId: {}, username: {}, gateId: {}", userId, username, gateId);
	}

	public long getUserId() {
		return userId;
	}

	public int getUserIdInt() {
		return (int) userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public int getGateId() {
		return gateId;
	}

	/** 兼容 room 侧 clientId 命名 */
	public int getClientId() {
		return gateId;
	}

	public void setGateId(int gateId) {
		this.gateId = gateId;
		updateActiveTime();
	}

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public void updateActiveTime() {
		this.lastActiveTime = System.currentTimeMillis();
	}

	public String getPendingToken() {
		return pendingToken;
	}

	public void setPendingToken(String pendingToken) {
		this.pendingToken = pendingToken;
	}

	public synchronized String consumePendingToken() {
		String token = this.pendingToken;
		this.pendingToken = null;
		return token;
	}

	public boolean getJoinGame() {
		return joinGame;
	}

	public void setJoinGame(boolean joinGame) {
		this.joinGame = joinGame;
	}

	public boolean getOffline() {
		return offline;
	}

	public void setOffline(boolean offline) {
		this.offline = offline;
	}

	public ModelProto.RoomRole getRole() {
		return ModelProto.RoomRole.newBuilder()
				.setRoleId(getUserIdInt())
				.build();
	}

	public void addTable(long tableId) {
		tables.add(tableId);
		logger.info("用户添加桌子, userId: {}, tableId: {}", userId, tableId);
	}

	public void removeTable(long tableId) {
		if (tables.remove(tableId)) {
			logger.info("用户移除桌子, userId: {}, tableId: {}", userId, tableId);
		}
	}

	public List<Long> getAllTables() {
		return new ArrayList<>(tables);
	}

	public void destroy() {
		tables.clear();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		User user = (User) o;
		return userId == user.userId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId);
	}
}
