package game.manager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import game.manager.model.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private int currentIndex = BASE_INDEX;

	/**
	 * 当前桌子头
	 */
	private String currHead;

	public TableManager() {
		tableMap = new ConcurrentHashMap<>();
		logger.info("桌子管理器初始化完成");
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
			logger.warn("桌子已存在,添加失败, tableId: {}", tableId);
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
			logger.warn("桌子不存在,无法删除, tableId: {}", tableId);
		}
		return removedTable;
	}

	/**
	 * 获取新的桌子ID
	 */
	public String getTableId() {
		synchronized (TableManager.class) {
			if (currentIndex >= Integer.MAX_VALUE - 1000) {
				logger.warn("桌子ID即将耗尽,考虑重置");
			}
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHH");
			String head = dateFormat.format(new Date());
			if (!head.equals(currHead)) {
				currHead = head;
				currentIndex = BASE_INDEX;
			}
			String tableId = currHead + ++currentIndex;
			logger.info("创建新桌子ID: {}", tableId);
			return tableId;
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
		logger.info("清理所有桌子,数量: {}", count);
	}
}