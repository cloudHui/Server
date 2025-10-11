package room.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import model.TableModel;
import proto.RoomProto;
import utils.other.excel.ExcelUtil;

/**
 * 房间模板管理
 */
public class RoomManager {

	private static final RoomManager instance = new RoomManager();
	private final ConcurrentHashMap<Integer, TableModel> tableModelMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, RoomProto.RoomTableInfo>> roomTables = new ConcurrentHashMap<>();

	public static RoomManager getInstance() {
		return instance;
	}

	public void init() {
		TableModel model;
		List<Object> properties = new ArrayList<>();
		//读取数据
		ExcelUtil.readExcelJavaValue("TableModel.xlsx", properties);
		synchronized (tableModelMap) {
			tableModelMap.clear();
			for (Object object : properties) {
				model = (TableModel) object;
				tableModelMap.put(model.getId(), model);
				roomTables.computeIfAbsent(model.getId(), k -> new ConcurrentHashMap<>());
			}
		}
	}

	/**
	 * 获取所有的展示房间
	 */
	public void getAllRoomTable(RoomProto.AckGetRoomList.Builder ack) {
		RoomProto.Room.Builder room;
		ConcurrentHashMap<String, RoomProto.RoomTableInfo> value;
		for (Map.Entry<Integer, ConcurrentHashMap<String, RoomProto.RoomTableInfo>> roomTable : roomTables.entrySet()) {
			room = RoomProto.Room.newBuilder();
			room.setRoomId(roomTable.getKey());
			value = roomTable.getValue();
			if (value != null && !value.isEmpty()) {
				room.addAllTables(value.values());
			}
			ack.addRoomList(room);
		}
	}

	/**
	 * 通过模板id 获取模板
	 */
	public TableModel getTableModel(int model) {
		return tableModelMap.get(model);
	}
}
