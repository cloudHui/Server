package game.manager.table.state;

import game.manager.table.Table;
import msg.registor.enums.TableState;

public interface TableHandle {

	boolean handleState(Table table);

	/**
	 * 默认通用处理
	 */
	default boolean handle(Table table) {
		TableState currState = table.getTableState();
		TableState next = currState.getNext();
		//有超时时间并且有默认下一个状态的默认处理等待超时切状态
		if (next != null && currState.getOverTime() > 0) {
			long now = System.currentTimeMillis();
			if (table.getStateStartTime() + table.getTableState().getOverTime() * 1000L >= now) {
				table.setTableState(next);
				table.setStateStartTime(now);
			}
		} else {
			return handleState(table);
		}
		return false;
	}
}
