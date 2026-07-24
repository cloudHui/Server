package lobby.manager.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lobby.db.CustomRoomRepository;
import lobby.db.SqliteDatabase;
import lobby.manager.User;
import model.tablemodel.TableModel;
import model.tablemodel.RobotRoomTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.LobbyProto;
import proto.ModelProto;
import tool.config.TableConfigManager;

/**
 * 房间模板与桌子管理（原 room.TableManager）
 */
public class TableManager {
	private static final Logger logger = LoggerFactory.getLogger(TableManager.class);
	private static final TableManager instance = new TableManager();

	private final TableConfigManager configManager;
	private final CustomRoomRepository customRoomRepository;
	private final Map<Integer, Map<Long, TableInfo>> roomTables = new ConcurrentHashMap<>();
	private final Map<Long, TableInfo> tableInfoMap = new ConcurrentHashMap<>();

	private TableManager() {
		configManager = new TableConfigManager();
		customRoomRepository = new CustomRoomRepository(SqliteDatabase.getInstance());
		if (configManager.loadFail()) {
			throw new RuntimeException("加载配置文件失败");
		}
		for (TableModel model : customRoomRepository.listEnabled()) {
			configManager.putRuntimeModel(model);
		}
		// 内置机器人模板不落库，服务启动时始终注册，保证大厅和游戏服都能看到。
		RobotRoomTemplates.register(configManager::putRuntimeModel);
		configManager.startWatch();
	}

	public static TableManager getInstance() {
		return instance;
	}

	public synchronized void init() {
		if (configManager.loadFail()) {
			throw new RuntimeException("加载配置文件失败");
		}
		// loadFail 会清空内存表，需重新装入库中自定义房间，再注册内置机器人模板。
		for (TableModel model : customRoomRepository.listEnabled()) {
			configManager.putRuntimeModel(model);
		}
		RobotRoomTemplates.register(configManager::putRuntimeModel);
		roomTables.clear();
		for (TableModel model : configManager.getAllTableModels().values()) {
			roomTables.computeIfAbsent(model.getId(), k -> new ConcurrentHashMap<>());
		}
		logger.info("房间管理器初始化完成,加载模板数量: {}", configManager.getAllTableModels().size());
	}

	public synchronized LobbyProto.AckRoomList getAllRoomTable() {
		LobbyProto.AckRoomList.Builder response = LobbyProto.AckRoomList.newBuilder();
		try {
			// 确保配置里的模板都有房间桶（热更/运行时模板）
			for (TableModel model : configManager.getAllTableModels().values()) {
				roomTables.computeIfAbsent(model.getId(), k -> new ConcurrentHashMap<>());
			}
			int totalRooms = 0;
			for (Map.Entry<Integer, Map<Long, TableInfo>> roomEntry : roomTables.entrySet()) {
				ModelProto.Room.Builder roomBuilder = ModelProto.Room.newBuilder();
				roomBuilder.setRoomId(roomEntry.getKey());
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

	public synchronized TableModel getTableModel(int modelId) {
		return configManager.getTableModel(modelId);
	}

	public synchronized void putRuntimeModel(TableModel model) {
		putRuntimeModel(model, "system");
	}

	public synchronized void putRuntimeModel(TableModel model, String createdBy) {
		configManager.putRuntimeModel(model);
		customRoomRepository.save(model, createdBy);
		roomTables.computeIfAbsent(model.getId(), k -> new ConcurrentHashMap<>());
	}

	public synchronized int nextRuntimeModelId() {
		int max = 10000;
		for (Integer id : configManager.getAllTableModels().keySet()) {
			if (id >= max) max = id + 1;
		}
		return max;
	}

	public synchronized TableInfo getTableById(long tableId) {
		return tableInfoMap.get(tableId);
	}

	public synchronized void removeTable(long tableId) {
		TableInfo tableInfo = tableInfoMap.remove(tableId);
		if (tableInfo != null) {
			// 清理玩家归属，避免登录/大厅仍挂着幽灵桌
			for (User user : new ArrayList<>(tableInfo.getTableRoles())) {
				if (user != null) {
					tableInfo.removeUser(user);
				}
			}
			Map<Long, TableInfo> tables = roomTables.get(tableInfo.getModel().getId());
			if (tables != null) {
				tables.remove(tableId);
			}
			logger.info("大厅移除桌子, tableId: {}", tableId);
		}
	}

	public synchronized TableInfo getCanJoinTable(int modelId) {
		Map<Long, TableInfo> model = roomTables.get(modelId);
		if (model == null || model.isEmpty()) {
			return null;
		}
		for (Map.Entry<Long, TableInfo> entry : model.entrySet()) {
			if (entry.getValue().canJoin()) {
				return entry.getValue();
			}
		}
		return null;
	}

	public synchronized TableInfo putRoomInfo(ModelProto.RoomTableInfo roomTable) {
		TableModel model = configManager.getTableModel(roomTable.getRoomId());
		if (model == null) {
			logger.warn("putRoomInfo失败, 房间模板不存在, roomId: {}", roomTable.getRoomId());
			return null;
		}
		TableInfo tableInfo = new TableInfo(roomTable.getTableId(), roomTable.getCreatorId(), model);
		tableInfo.setOwnerId(roomTable.getOwnerId());
		roomTables.computeIfAbsent(model.getId(), k -> new ConcurrentHashMap<>())
				.put(tableInfo.getTableId(), tableInfo);
		tableInfoMap.put(tableInfo.getTableId(), tableInfo);
		return tableInfo;
	}

	public synchronized void clearAllTables() {
		int count = tableInfoMap.size();
		tableInfoMap.clear();
		for (Map<Long, TableInfo> tables : roomTables.values()) {
			tables.clear();
		}
		logger.info("已清空所有桌子,数量: {}", count);
	}

	public synchronized void restoreTables(List<ModelProto.RoomTableInfo> tableList) {
		if (tableList == null || tableList.isEmpty()) {
			return;
		}
		for (ModelProto.RoomTableInfo info : tableList) {
			putRoomInfo(info);
		}
		logger.info("恢复桌子完成,数量: {}", tableList.size());
	}
}
