package room.manager;

import utils.utils.excel.ExcelUtil;

/**
 * 房间模板管理
 */
public class RoomModelManager {

	private final RoomModelManager instance = new RoomModelManager();

	public RoomModelManager getInstance() {
		return instance;
	}

	public void readTableModel() {
	}

	public static void main(String[] args){
		ExcelUtil.readExcelCreateJavaHead("TableModel.xlsx", "room");
	}
}
