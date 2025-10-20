package game.manager.table.state;

import java.util.HashMap;
import java.util.Map;

import game.manager.table.Table;
import game.manager.table.TableUser;
import msg.registor.HandleTypeRegister;
import msg.registor.enums.TableState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author admin
 * @className TableStateHandleManager
 * @description
 * @createDate 2025/10/20 16:53
 */
public class TableStateHandleManager {

	private static final Logger logger = LoggerFactory.getLogger(TableUser.class);

	private static final Map<TableState, TableHandle> STATE_TABLE_HANDLE_MAP = new HashMap<>();

	static {
		HandleTypeRegister.initFactoryEnum(TableStateHandleManager.class, STATE_TABLE_HANDLE_MAP);
	}

	/**
	 * 桌子状态处理器处理
	 *
	 * @param table 牌局
	 */
	public static boolean handle(Table table) {
		TableHandle tableHandle = STATE_TABLE_HANDLE_MAP.get(table.getTableState());

		if (tableHandle == null) {
			logger.error("table:{} state:{} no handle", table.getTableId(), table.getTableState());
			return true;
		}
		boolean exit = false;
		if (logger.isDebugEnabled()) {
			logger.debug("桌子状态处理开始, tableId: {}, state:{}", table.getTableId(), table.getTableState());
		}
		try {
			exit = tableHandle.handle(table);
		} catch (Exception e) {
			table.addErrorTime();
			e.printStackTrace();
		}
		if (logger.isDebugEnabled()) {
			logger.debug("桌子状态处理结束, tableId: {}, state:{}", table.getTableId(), table.getTableState());
		}
		return exit;
	}
}
