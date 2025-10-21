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
		//有超时时间并且有默认下一个状态的默认处理等待超时切状态
		if (currState.getOverTime() > 0) {
			long now = System.currentTimeMillis();
			if (table.getStateStartTime() + table.getTableState().getOverTime() * 1000L >= now) {
				TableState next = currState.getNext();
				if (next != null) {
					table.setTableState(next);
					table.setStateStartTime(now);
				} else {
					overTime(table);
				}
			}
			return false;
		}
		return handleState(table);
	}

	/**
	 * 超时处理
	 */
	default void overTime(Table table) {
	}
}
