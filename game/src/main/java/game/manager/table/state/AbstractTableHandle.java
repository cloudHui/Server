package game.manager.table.state;

import game.manager.table.Table;
import msg.registor.enums.TableState;

/**
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 * @className AbstractTableHandle
 * @description 桌子状态处理器抽象类
 * @createDate 2025/10/20 16:53
 */
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
	 * 
	 * @param table 桌子
	 * @return 是否退出循环
	 */
	public boolean handle(Table table) {
		TableState currState = table.getTableState();
		// 有超时时间：仅在已超过等待时长后切下一状态或调用 overTime
		if (currState.getOverTime() > 0) {
			long now = System.currentTimeMillis();
			long deadline = table.getStateStartTime() + currState.getOverTime() * 1000L;
			if (now >= deadline) {
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
	 * 
	 * @param table 桌子
	 */
	protected void overTime(Table table) {
	}
}
