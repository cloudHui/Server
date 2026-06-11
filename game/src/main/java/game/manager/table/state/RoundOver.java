package game.manager.table.state;

import game.manager.table.Table;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * 牌局结束阶段：等待玩家准备下一局。
 * 当前多局逻辑由 {@link TableOverBridge} 处理, 此状态为备用。
 */
@ProcessEnum(TableState.ROUND_OVER)
public class RoundOver extends AbstractTableHandle {

	@Override
	public boolean onTiming(Table table) {
		// 无超时状态, 每tick检查。由 TableOverBridge 的多局逻辑控制转换。
		return false;
	}

	@Override
	protected void overTime(Table table) {
		// 备用: 如果意外进入此状态且设置了超时, 自动进入下一局
		table.upNextState();
	}
}
