package game.manager;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import game.Game;
import game.manager.model.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.other.TimeUtil;

/**
 * 桌子管理器
 * 负责游戏桌子的创建、管理和生命周期控制
 */
public class TableManager {
	private static final Logger logger = LoggerFactory.getLogger(TableManager.class);

	// 初始桌子序号
	private static final int BASE_INDEX = 100000;
	private final Map<String, Table> tableMap;

	/**
	 * 当前初始化桌子号
	 */
	private int currentIndex;

	// 桌子号的前缀
	private String idPrefix;

	public TableManager() {
		tableMap = new ConcurrentHashMap<>();
		resetTableId();
		registerResetTableTask();
		logger.info("桌子管理器初始化完成");
	}

	public void setIdPrefix(String idPrefix) {
		this.idPrefix = idPrefix;
	}

	/**
	 * 添加桌子
	 */
	public void addTable(Table table) {
		if (table == null) {
			logger.warn("尝试添加空桌子");
			return;
		}

		String tableId = table.getTableId();
		Table existingTable = tableMap.putIfAbsent(tableId, table);

		if (existingTable != null) {
			logger.warn("桌子已存在，添加失败, tableId: {}", tableId);
		} else {
			logger.debug("添加新桌子, tableId: {}", tableId);
		}
	}

	/**
	 * 获取桌子
	 */
	public Table getTable(String tableId) {
		Table table = tableMap.get(tableId);
		if (table == null) {
			logger.debug("桌子不存在, tableId: {}", tableId);
		}
		return table;
	}

	/**
	 * 删除桌子
	 */
	public Table removeTable(String tableId) {
		Table removedTable = tableMap.remove(tableId);
		if (removedTable != null) {
			logger.info("删除桌子, tableId: {}", tableId);
		} else {
			logger.warn("桌子不存在，无法删除, tableId: {}", tableId);
		}
		return removedTable;
	}

	/**
	 * 注册重置桌子ID任务
	 * 每天0点重置桌子号和其他参数
	 */
	private void registerResetTableTask() {
		long nextZero = TimeUtil.curZeroHourTime(System.currentTimeMillis()) + TimeUtil.DAY;
		Game.getInstance().registerTimer(nextZero, TimeUtil.DAY, -1, manager -> {
			resetTableId();
			return false;
		}, this);

		logger.debug("已注册重置桌子ID任务，下次执行时间: {}", new Timestamp(nextZero));
	}

	/**
	 * 获取新的桌子ID
	 */
	public String getTableId() {
		synchronized (TableManager.class) {
			if (currentIndex >= Integer.MAX_VALUE - 1000) {
				logger.warn("桌子ID即将耗尽，考虑重置");
			}

			String tableId = idPrefix + currentIndex++;
			logger.info("创建新桌子ID: {}", tableId);
			return tableId;
		}
	}

	/**
	 * 重置ID前缀和索引
	 * 使用日期时间作为前缀，确保ID的唯一性
	 */
	public void resetTableId() {
		synchronized (TableManager.class) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHH");
			setIdPrefix(dateFormat.format(new Date()));
			currentIndex = BASE_INDEX;
			logger.info("重置桌子ID参数, 时间: {}, 前缀: {}, 基础索引: {}",
					new Timestamp(System.currentTimeMillis()), idPrefix, currentIndex);
		}
	}

	/**
	 * 获取当前桌子数量
	 */
	public int getTableCount() {
		return tableMap.size();
	}

	/**
	 * 清理所有桌子（用于服务器关闭时）
	 */
	public void clearAllTables() {
		int count = tableMap.size();
		tableMap.clear();
		logger.info("清理所有桌子，数量: {}", count);
	}
}