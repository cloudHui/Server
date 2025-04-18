package room.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.TableModel;
import utils.other.excel.ExcelUtil;

/**
 * 房间模板管理
 */
public class RoomModelManager {

	private static final RoomModelManager instance = new RoomModelManager();

	public static RoomModelManager getInstance() {
		return instance;
	}

	private final Map<Integer, TableModel> tableModelMap = new HashMap<>();

	public void init() {
		readTableModel();
	}

	private void readTableModel() {
		//读取文件生成结构
		ExcelUtil.readExcelCreateJavaHead("TableModel.xlsx", "room");

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
	 * 获取所有模板
	 */
	public List<TableModel> getModels() {
		return new ArrayList<>(tableModelMap.values());
	}

	/**
	 * 通过模板id 获取模板
	 */
	public TableModel getTableModel(int model) {
		return tableModelMap.get(model);
	}
}
