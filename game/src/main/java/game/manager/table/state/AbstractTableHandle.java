package game.manager.table.state;

import game.manager.table.Table;
import msg.registor.enums.TableState;

public abstract class AbstractTableHandle {

	/**
	 * 状态实时检测反应和处理
	 *
	 * @param table 桌子
	 * @return 是否退出循环
	 */
	protected boolean onTiming(Table table) {
		return false;
	}

	/**
	 * 默认通用处理
	 */
	public boolean handle(Table table) {
		TableState currState = table.getTableState();
		//有超时时间并且有默认下一个状态的默认处理等待超时切状态
		if (currState.getOverTime() > 0) {
			long now = System.currentTimeMillis();
			if (table.getStateStartTime() + table.getTableState().getOverTime() * 1000L >= now) {
				TableState next = currState.getNext();
				if (next != null) {
					table.upNextStateWithTime(next, now);
				} else {
					overTime(table);
				}
			}
			return false;
		}
		return onTiming(table);
	}

	/**
	 * 超时处理
	 */
	protected void overTime(Table table) {
	}
}
