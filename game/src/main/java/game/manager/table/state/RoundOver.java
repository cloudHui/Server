package game.manager.table.state;

import game.manager.table.Table;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * @author admin
 * @className RoundOver
 * @description 牌局结束阶段：依赖 {@link AbstractTableHandle} 的超时逻辑切入
 *              {@link TableState#ROUND_OVER}。
 * @createDate 2025/10/20 16:57
 */
@ProcessEnum(TableState.ROUND_OVER)
public class RoundOver extends AbstractTableHandle {

	@Override
	public boolean onTiming(Table table) {
		return false;
	}

	@Override
	protected void overTime(Table table) {

	}
}
