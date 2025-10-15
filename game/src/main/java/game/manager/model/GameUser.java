package game.manager.model;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 游戏用户模型
 * 代表一个游戏玩家的状态和信息
 */
public class GameUser {
	private static final Logger logger = LoggerFactory.getLogger(GameUser.class);

	private final Set<String> tableIds = new HashSet<>();
	private int userId;
	private boolean online;
	private boolean seated;
	private long diamond;

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
		logger.debug("设置用户ID: {}", userId);
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		boolean changed = this.online != online;
		this.online = online;
		if (changed) {
			logger.debug("更新用户在线状态, userId: {}, online: {}", userId, online);
		}
	}

	public boolean isSeated() {
		return seated;
	}

	public void setSeated(boolean seated) {
		boolean changed = this.seated != seated;
		this.seated = seated;
		if (changed) {
			logger.debug("更新用户入座状态, userId: {}, seated: {}", userId, seated);
		}
	}

	public long getDiamond() {
		return diamond;
	}

	public void setDiamond(long diamond) {
		long oldValue = this.diamond;
		this.diamond = diamond;
		if (oldValue != diamond) {
			logger.debug("更新用户钻石数量, userId: {}, old: {}, new: {}", userId, oldValue, diamond);
		}
	}

	public Set<String> getTableIds() {
		return new HashSet<>(tableIds); // 返回副本避免外部修改
	}

	/**
	 * 添加桌子ID到用户
	 */
	public void addTable(String tableId) {
		if (tableId == null) {
			logger.warn("尝试添加空桌子ID到用户, userId: {}", userId);
			return;
		}

		if (tableIds.add(tableId)) {
			logger.debug("用户添加桌子, userId: {}, tableId: {}", userId, tableId);
		}
	}

	/**
	 * 从用户移除桌子ID
	 */
	public void removeTable(String tableId) {
		if (tableId == null) {
			logger.warn("尝试从用户移除空桌子ID, userId: {}", userId);
			return;
		}

		if (tableIds.remove(tableId)) {
			logger.debug("用户移除桌子, userId: {}, tableId: {}", userId, tableId);
		}
	}

	/**
	 * 检查用户是否在指定桌子中
	 */
	public boolean isInTable(String tableId) {
		return tableIds.contains(tableId);
	}

	/**
	 * 获取用户所在的桌子数量
	 */
	public int getTableCount() {
		return tableIds.size();
	}

	/**
	 * 清理用户所有桌子关联
	 */
	public void clearTables() {
		int count = tableIds.size();
		tableIds.clear();
		logger.debug("清理用户所有桌子关联, userId: {}, 数量: {}", userId, count);
	}

	@Override
	public String toString() {
		return String.format("GameUser{userId=%d, online=%s, seated=%s, diamond=%d, tableCount=%d}",
				userId, online, seated, diamond, tableIds.size());
	}
}