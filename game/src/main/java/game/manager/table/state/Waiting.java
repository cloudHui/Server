package game.manager.table.state;

import game.manager.table.Table;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * @author admin
 * @className Waiting
 * @description 等待阶段：依赖 {@link AbstractTableHandle} 的超时逻辑切入
 *              {@link TableState#WAITING}。
 * @createDate 2025/10/20 16:57
 */
@ProcessEnum(TableState.WAITING)
public class Waiting extends AbstractTableHandle {

	@Override
	public boolean onTiming(Table table) {
		if (table.sitFull()) {
			if (table.getTableModel().getType() == 1) {
				// 麻将：发牌后进入MJ_DEAL
				table.sendMjCard();
				table.getMjContext().resetRound();
				table.getOp().setCurrOpSeat(0);
				table.upNextState(TableState.MJ_DEAL);
			} else {
				// 斗地主：原有逻辑
				table.upNextState();
				table.sendCard();
			}
			return false;
		}
		return table.isEmpty();
	}
}
