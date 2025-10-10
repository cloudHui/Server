package room.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import model.TableModel;
import proto.RoomProto;
import room.model.RoomTable;
import utils.other.excel.ExcelUtil;

/**
 * 房间模板管理
 */
public class RoomManager {

	private static final RoomManager instance = new RoomManager();
	private final ConcurrentHashMap<Integer, TableModel> tableModelMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, RoomTable>> roomTables = new ConcurrentHashMap<>();

	public static RoomManager getInstance() {
		return instance;
	}

	public void init() {
		//读取文件生成结构
		ExcelUtil.readExcelCreateJavaHead("TableModel.xlsx", "room");

		readTableModel();
	}

	private void readTableModel() {
		TableModel model;
		List<Object> prperties = new ArrayList<>();
		//读取数据
		ExcelUtil.readExcelJavaValue("TableModel.xlsx", prperties);
		synchronized (tableModelMap) {
			tableModelMap.clear();
			for (Object object : prperties) {
				model = (TableModel) object;
				tableModelMap.put(model.getId(), model);
			}
		}
	}

	/**
	 * 获取所有的展示房间
	 */
	public void getAllRoomTable(RoomProto.AckGetRoomList.Builder ack) {
		RoomProto.Room.Builder room;
		ConcurrentHashMap<String, RoomTable> value;
		for (Map.Entry<Integer, ConcurrentHashMap<String, RoomTable>> roomTable : roomTables.entrySet()) {
			room = RoomProto.Room.newBuilder();
			room.setConfigTypeId(roomTable.getKey());
			value = roomTable.getValue();

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
