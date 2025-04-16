package room.manager;

import java.util.ArrayList;
import java.util.List;

import room.model.TableModel;
import utils.other.excel.ExcelUtil;

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

	public static void main(String[] args) {
		//读取文件生成结构
		ExcelUtil.readExcelCreateJavaHead("TableModel.xlsx", "room");

		TableModel model;
		List<Object> prperties = new ArrayList<>();
		//读取数据
		ExcelUtil.readExcelJavaValue("TableModel.xlsx", prperties);
		for (Object object : prperties) {
			model = (TableModel) object;
			System.out.println(model.toString());
		}
	}
}
