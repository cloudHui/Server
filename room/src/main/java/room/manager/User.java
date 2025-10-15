package room.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;

/**
 * 用户信息模型
 * 管理用户在房间服务器中的状态和数据
 */
public class User {
	private static final Logger logger = LoggerFactory.getLogger(User.class);

	private final int userId;
	private ServerProto.RoomRole role;
	private boolean offline = false;
	private final Map<String, ServerProto.RoomTableInfo> tables = new HashMap<>();

	public User(int userId) {
		this.userId = userId;
		logger.debug("创建用户实例, userId: {}", userId);
	}

	public int getUserId() {
		return userId;
	}

	public void setRole(ServerProto.RoomRole role) {
		this.role = role;
		logger.debug("设置用户角色, userId: {}, role: {}", userId, role);
	}

	public ServerProto.RoomRole getRole() {
		return role;
	}

	public void setOffline(boolean offline) {
		this.offline = offline;
		logger.debug("设置用户离线状态, userId: {}, offline: {}", userId, offline);
	}

	public boolean isOffline() {
		return offline;
	}

	/**
	 * 添加用户桌子
	 */
	public void addTable(ServerProto.RoomTableInfo tableInfo) {
		String tableId = tableInfo.getTableId().toStringUtf8();
		tables.put(tableId, tableInfo);
		logger.debug("用户添加桌子, userId: {}, tableId: {}", userId, tableId);
	}

	/**
	 * 移除用户桌子
	 */
	public void removeTable(String tableId) {
		ServerProto.RoomTableInfo removed = tables.remove(tableId);
		if (removed != null) {
			logger.debug("用户移除桌子, userId: {}, tableId: {}", userId, tableId);
		} else {
			logger.warn("用户桌子不存在，无法移除, userId: {}, tableId: {}", userId, tableId);
		}
	}

	/**
	 * 获取用户所有桌子
	 */
	public List<ServerProto.RoomTableInfo> getAllTables() {
		return new ArrayList<>(tables.values());
	}

	/**
	 * 获取桌子数量
	 */
	public int getTableCount() {
		return tables.size();
	}

	/**
	 * 清理用户资源
	 */
	public void destroy() {
		tables.clear();
		logger.debug("清理用户资源, userId: {}", userId);
	}
}