package game.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import game.manager.table.Table;
import model.TableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import proto.ServerProto;
import utils.other.excel.ExcelUtil;

/**
 * 桌子管理器
 * 负责游戏桌子的创建、管理和生命周期控制
 */
public class TableManager {
	private static final Logger logger = LoggerFactory.getLogger(TableManager.class);

	// 初始桌子序号
	private static final int BASE_INDEX = 100000;
	private final Map<Long, Table> tableMap;

	/**
	 * 当前初始化桌子号
	 */
	private int currentIndex = BASE_INDEX;

	private final Map<Integer, TableModel> tableModelMap = new HashMap<>();

	public TableManager() {
		tableMap = new ConcurrentHashMap<>();
		init();
		logger.info("桌子管理器初始化完成");
	}


	/**
	 * 初始化房间管理器
	 * 从Excel文件加载房间配置
	 */
	public synchronized void init() {
		try {
			List<Object> properties = new ArrayList<>();

			// 读取Excel配置
			ExcelUtil.readExcelJavaValue("TableModel.xlsx", properties);

			synchronized (tableModelMap) {
				//Todo 重新load 以后之前的房间尽量打完删除
				tableModelMap.clear();

				for (Object object : properties) {
					TableModel model = (TableModel) object;
					tableModelMap.put(model.getId(), model);
					logger.debug("加载房间模板, id: {}", model.getId());
				}
			}

			logger.info("房间管理器初始化完成,加载模板数量: {}", tableModelMap.size());
		} catch (Exception e) {
			logger.error("房间管理器初始化失败", e);
			throw new RuntimeException("房间管理器初始化失败", e);
		}
	}

	/**
	 * 添加桌子
	 */
	public void addTable(Table table) {
		if (table == null) {
			logger.warn("尝试添加空桌子");
			return;
		}

		long tableId = table.getTableId();
		Table existingTable = tableMap.get(tableId);

		if (existingTable != null) {
			logger.warn("桌子已存在,添加失败, tableId: {}", tableId);
		} else {
			tableMap.put(tableId, table);
			logger.debug("添加新桌子, tableId: {}", tableId);
		}
	}

	public static void main(String[] args) {
		Map<Integer, Integer> map = new HashMap<>();
		Integer integer = map.putIfAbsent(1, 2);

		System.out.println(integer);
	}

	/**
	 * 获取桌子
	 */
	public Table getTable(long tableId) {
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
	private long getTableId() {
		logger.info("创建新桌子ID: {}", ++currentIndex);
		return currentIndex;

	}

	/**
	 * 创建桌子
	 *
	 * @param roomId 桌子类型
	 * @param role   创建的玩家
	 * @return 桌子实例
	 */
	public Table createTable(int roomId, ModelProto.RoomRole role) {
		synchronized (TableManager.class) {
			TableModel model = tableModelMap.get(roomId);
			Table table = new Table(getTableId(), model, role);
			addTable(table);
			return table;
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