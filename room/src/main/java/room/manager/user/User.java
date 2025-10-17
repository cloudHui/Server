package room.manager.user;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;
import room.manager.table.TableInfo;
import room.manager.table.TableManager;

/**
 * 用户信息模型
 * 管理用户在房间服务器中的状态和数据
 */
public class User {
	private static final Logger logger = LoggerFactory.getLogger(User.class);

	private final int userId;
	private boolean joinGame;
	private ServerProto.RoomRole role;
	private boolean offline = false;
	private final int clientId;
	private final Set<String> tables = new HashSet<>();

	public User(int userId, int clientId) {
		this.userId = userId;
		this.clientId = clientId;
		logger.debug("创建用户实例, userId: {}", userId);
	}

	public int getUserId() {
		return userId;
	}

	public int getClientId() {
		return clientId;
	}

	public void setRole(ServerProto.RoomRole role) {
		this.role = role;
		logger.info("设置用户角色, userId: {}, role: {}", userId, role);
	}

	public ServerProto.RoomRole getRole() {
		return role;
	}

	public void setOffline(boolean offline) {
		this.offline = offline;
		logger.info("设置用户离线状态, userId: {}, offline: {}", userId, offline);
	}

	public boolean getOffline() {
		return offline;
	}

	public boolean getJoinGame() {
		return joinGame;
	}

	public void setJoinGame(boolean joinGame) {
		this.joinGame = joinGame;
		logger.info("joinGame, userId: {}, joinGame: {}", userId, joinGame);
	}

	/**
	 * 添加用户桌子
	 */
	public void addTable(String tableId) {
		tables.add(tableId);
		logger.info("用户添加桌子, userId: {}, tableId: {}", userId, tableId);
	}

	/**
	 * 移除用户桌子
	 */
	public void removeTable(String tableId) {
		boolean removed = tables.remove(tableId);
		if (removed) {
			logger.info("用户移除桌子, userId: {}, tableId: {}", userId, tableId);
		} else {
			logger.error("用户桌子不存在,无法移除, userId: {}, tableId: {}", userId, tableId);
		}
	}

	/**
	 * 获取用户所有桌子
	 */
	public List<ServerProto.RoomTableInfo> getAllTables() {
		List<ServerProto.RoomTableInfo> tableInfos = new ArrayList<>();
		TableInfo tableById;
		List<String> remove = new ArrayList<>();
		for (String table : tables) {
			tableById = TableManager.getInstance().getTableById(table);
			if (tableById != null) {
				tableInfos.add(tableById.getTableInfo());
			} else {
				remove.add(table);
				logger.info("getAllTables no table:{}, userId: {}", table, userId);
			}
		}
		tables.removeAll(remove);
		return tableInfos;
	}

	/**
	 * 清理用户资源
	 */
	public void destroy() {
		tables.clear();
		logger.info("清理用户资源, userId: {}", userId);
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