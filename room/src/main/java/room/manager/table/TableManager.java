package room.manager.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import model.TableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.RoomProto;
import proto.ServerProto;
import utils.other.excel.ExcelUtil;

/**
 * 房间模板管理器
 * 负责加载和管理房间配置模板
 */
public class TableManager {
	private static final Logger logger = LoggerFactory.getLogger(TableManager.class);
	private static final TableManager instance = new TableManager();

	private final Map<Integer, TableModel> tableModelMap = new HashMap<>();
	private final Map<Integer, Map<String, TableInfo>> roomTables = new HashMap<>();
	private final Map<String, TableInfo> tableInfoMap = new HashMap<>();

	public static TableManager getInstance() {
		return instance;
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
				roomTables.clear();

				for (Object object : properties) {
					TableModel model = (TableModel) object;
					tableModelMap.put(model.getId(), model);
					roomTables.computeIfAbsent(model.getId(), k -> new ConcurrentHashMap<>());
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
	 * 获取所有展示房间信息
	 */
	public synchronized void getAllRoomTable(RoomProto.AckGetRoomList.Builder response) {
		try {
			int totalRooms = 0;

			for (Map.Entry<Integer, Map<String, TableInfo>> roomEntry : roomTables.entrySet()) {
				RoomProto.Room.Builder roomBuilder = RoomProto.Room.newBuilder();
				roomBuilder.setRoomId(roomEntry.getKey());

				Map<String, TableInfo> tables = roomEntry.getValue();
				if (tables != null && !tables.isEmpty()) {
					for (Map.Entry<String, TableInfo> entry : tables.entrySet()) {
						roomBuilder.addTables(entry.getValue().getTableInfo());
					}
					totalRooms += tables.size();
				}

				response.addRoomList(roomBuilder);
			}

			logger.debug("返回房间列表,房间类型数: {}, 总房间数: {}", roomTables.size(), totalRooms);
		} catch (Exception e) {
			logger.error("获取房间列表失败", e);
			throw new RuntimeException("获取房间列表失败", e);
		}
	}

	/**
	 * 通过模板ID获取房间模板
	 */
	public TableModel getTableModel(int modelId) {
		TableModel model = tableModelMap.get(modelId);
		if (model == null) {
			logger.warn("房间模板不存在, modelId: {}", modelId);
		}
		return model;
	}

	/**
	 * 通过桌子号取桌子
	 *
	 * @param tableId 桌子号
	 */
	public TableInfo getTableById(String tableId) {
		return tableInfoMap.get(tableId);
	}

	/**
	 * 删除桌子 通过桌子号
	 *
	 * @param tableId 桌子号
	 */
	public void removeTable(String tableId) {
		TableInfo tableInfo = tableInfoMap.remove(tableId);
		if (tableInfo != null) {
			roomTables.getOrDefault(tableInfo.getModel().getId(), new HashMap<>()).remove(tableId);
		}
	}

	/**
	 * 获取模板可加入房间
	 */
	public synchronized TableInfo getCanJoinTable(int modelId) {
		Map<String, TableInfo> model = roomTables.get(modelId);
		if (model == null || model.isEmpty()) {
			logger.warn("没有可加入房间 create, modelId: {}", modelId);
			return null;
		}
		for (Map.Entry<String, TableInfo> entry : model.entrySet()) {
			if (entry.getValue().canJoin()) {
				return entry.getValue();
			}
		}
		return null;
	}


	/**
	 * 存房间信息
	 */
	public synchronized TableInfo putRoomInfo(ServerProto.RoomTableInfo roomTable) {
		TableInfo tableInfo = new TableInfo(roomTable.getTableId().toStringUtf8(), roomTable.getCreatorId(), tableModelMap.get(roomTable.getRoomId()));
		roomTables.computeIfAbsent(tableInfo.getModel().getId(), k -> new HashMap<>()).put(tableInfo.getTableId(), tableInfo);
		tableInfoMap.put(tableInfo.getTableId(), tableInfo);
		return tableInfo;
	}
}