package room.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import model.TableModel;
import proto.RoomProto;
import proto.ServerProto;
import utils.other.excel.ExcelUtil;

/**
 * 房间模板管理器
 * 负责加载和管理房间配置模板
 */
public class RoomManager {
	private static final Logger logger = LoggerFactory.getLogger(RoomManager.class);
	private static final RoomManager instance = new RoomManager();

	private final ConcurrentHashMap<Integer, TableModel> tableModelMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, ServerProto.RoomTableInfo>> roomTables = new ConcurrentHashMap<>();

	public static RoomManager getInstance() {
		return instance;
	}

	/**
	 * 初始化房间管理器
	 * 从Excel文件加载房间配置
	 */
	public void init() {
		try {
			List<Object> properties = new ArrayList<>();

			// 读取Excel配置
			ExcelUtil.readExcelJavaValue("TableModel.xlsx", properties);

			synchronized (tableModelMap) {
				tableModelMap.clear();
				roomTables.clear();

				for (Object object : properties) {
					TableModel model = (TableModel) object;
					tableModelMap.put(model.getId(), model);
					roomTables.computeIfAbsent(model.getId(), k -> new ConcurrentHashMap<>());
					logger.debug("加载房间模板, id: {}", model.getId());
				}
			}

			logger.info("房间管理器初始化完成，加载模板数量: {}", tableModelMap.size());
		} catch (Exception e) {
			logger.error("房间管理器初始化失败", e);
			throw new RuntimeException("房间管理器初始化失败", e);
		}
	}

	/**
	 * 获取所有展示房间信息
	 */
	public void getAllRoomTable(RoomProto.AckGetRoomList.Builder response) {
		try {
			int totalRooms = 0;

			for (Map.Entry<Integer, ConcurrentHashMap<String, ServerProto.RoomTableInfo>> roomEntry : roomTables.entrySet()) {
				RoomProto.Room.Builder roomBuilder = RoomProto.Room.newBuilder();
				roomBuilder.setRoomId(roomEntry.getKey());

				ConcurrentHashMap<String, ServerProto.RoomTableInfo> tables = roomEntry.getValue();
				if (tables != null && !tables.isEmpty()) {
					roomBuilder.addAllTables(tables.values());
					totalRooms += tables.size();
				}

				response.addRoomList(roomBuilder);
			}

			logger.debug("返回房间列表，房间类型数: {}, 总房间数: {}",
					roomTables.size(), totalRooms);
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
	 * 获取所有模板ID
	 */
	public List<Integer> getAllModelIds() {
		return new ArrayList<>(tableModelMap.keySet());
	}
}