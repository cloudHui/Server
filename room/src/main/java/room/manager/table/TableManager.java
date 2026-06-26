package room.manager.table;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import model.tablemodel.TableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import proto.RoomProto;
import tool.config.TableConfigManager;

/**
 * 房间模板管理器
 * 负责加载和管理房间配置模板
 */
public class TableManager {
	private static final Logger logger = LoggerFactory.getLogger(TableManager.class);
	private static final TableManager instance = new TableManager();

	private final TableConfigManager configManager;
	private final Map<Integer, Map<Long, TableInfo>> roomTables = new ConcurrentHashMap<>();
	private final Map<Long, TableInfo> tableInfoMap = new ConcurrentHashMap<>();

	private TableManager() {
		configManager = new TableConfigManager();
		if (configManager.loadFail()) {
			throw new RuntimeException("加载配置文件失败");
		}
		configManager.startWatch();
	}

	public static TableManager getInstance() {
		return instance;
	}

	/**
	 * 初始化房间管理器
	 */
	public synchronized void init() {
		if (configManager.loadFail()) {
			throw new RuntimeException("加载配置文件失败");
		}

		roomTables.clear();
		for (TableModel model : configManager.getAllTableModels().values()) {
			roomTables.computeIfAbsent(model.getId(), k -> new ConcurrentHashMap<>());
		}

		logger.info("房间管理器初始化完成,加载模板数量: {}", configManager.getAllTableModels().size());
	}

	/**
	 * 获取所有展示房间信息
	 */
	public synchronized RoomProto.AckRoomList getAllRoomTable() {
		RoomProto.AckRoomList.Builder response = RoomProto.AckRoomList.newBuilder();
		try {
			int totalRooms = 0;

			for (Map.Entry<Integer, Map<Long, TableInfo>> roomEntry : roomTables.entrySet()) {
				ModelProto.Room.Builder roomBuilder = ModelProto.Room.newBuilder();
				roomBuilder.setRoomId(roomEntry.getKey());

				// 设置游戏类型
				TableModel tableModel = configManager.getTableModel(roomEntry.getKey());
				if (tableModel != null) {
					roomBuilder.setGameType(tableModel.getType());
				}

				Map<Long, TableInfo> tables = roomEntry.getValue();
				if (tables != null && !tables.isEmpty()) {
					for (Map.Entry<Long, TableInfo> entry : tables.entrySet()) {
						roomBuilder.addTables(entry.getValue().getTableInfo());
					}
					totalRooms += tables.size();
				}

				response.addRoomList(roomBuilder);
			}

			logger.debug("返回房间列表,房间类型数: {}, 总房间数: {}", roomTables.size(), totalRooms);
		} catch (Exception e) {
			logger.error("获取房间列表失败", e);
		}
		return response.build();
	}

	/**
	 * 通过模板ID获取房间模板
	 */
	public synchronized TableModel getTableModel(int modelId) {
		return configManager.getTableModel(modelId);
	}

	/**
	 * 通过桌子号取桌子
	 *
	 * @param tableId 桌子号
	 */
	public synchronized TableInfo getTableById(long tableId) {
		return tableInfoMap.get(tableId);
	}

	/**
	 * 删除桌子 通过桌子号
	 *
	 * @param tableId 桌子号
	 */
	public synchronized void removeTable(long tableId) {
		TableInfo tableInfo = tableInfoMap.remove(tableId);
		if (tableInfo != null) {
			Map<Long, TableInfo> tables = roomTables.get(tableInfo.getModel().getId());
			if (tables != null) {
				tables.remove(tableId);
			}
		}
	}

	/**
	 * 获取模板可加入房间
	 */
	public synchronized TableInfo getCanJoinTable(int modelId) {
		Map<Long, TableInfo> model = roomTables.get(modelId);
		if (model == null || model.isEmpty()) {
			logger.warn("没有可加入房间 create, modelId: {}", modelId);
			return null;
		}
		for (Map.Entry<Long, TableInfo> entry : model.entrySet()) {
			if (entry.getValue().canJoin()) {
				return entry.getValue();
			}
		}
		return null;
	}


	/**
	 * 存房间信息
	 */
	public synchronized TableInfo putRoomInfo(ModelProto.RoomTableInfo roomTable) {
		TableModel model = configManager.getTableModel(roomTable.getRoomId());
		if (model == null) {
			logger.warn("putRoomInfo失败, 房间模板不存在, roomId: {}", roomTable.getRoomId());
			return null;
		}
		TableInfo tableInfo = new TableInfo(roomTable.getTableId(), roomTable.getCreatorId(), model);
		tableInfo.setOwnerId(roomTable.getOwnerId());
		roomTables.computeIfAbsent(model.getId(), k -> new ConcurrentHashMap<>()).put(tableInfo.getTableId(), tableInfo);
		tableInfoMap.put(tableInfo.getTableId(), tableInfo);
		return tableInfo;
	}

	/**
	 * 清空所有桌子（Game断线时调用，保留房间模板）
	 */
	public synchronized void clearAllTables() {
		int count = tableInfoMap.size();
		tableInfoMap.clear();
		for (Map<Long, TableInfo> tables : roomTables.values()) {
			tables.clear();
		}
		logger.info("已清空所有桌子,数量: {}", count);
	}

	/**
	 * 批量导入桌子信息（Room重启恢复时调用）
	 */
	public synchronized void restoreTables(List<ModelProto.RoomTableInfo> tableList) {
		if (tableList == null || tableList.isEmpty()) return;
		for (ModelProto.RoomTableInfo info : tableList) {
			putRoomInfo(info);
		}
		logger.info("恢复桌子完成,数量: {}", tableList.size());
	}
}