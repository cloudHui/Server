package game.manager.table.state;

import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.ddz.DdzPlayService;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * 等待玩家出牌；超时自动处理（首家出最小单张，否则视为过）。
 */
@ProcessEnum(TableState.IDLE_CARD)
public class IdleCardPlay extends AbstractTableHandle {

	@Override
	public void overTime(Table table) {
		int seat = table.getOp().getCurrOpSeat();
		TableUser u = table.getSeatUser(seat);
		if (u == null) {
			return;
		}
		if (DdzPlayService.autoPlayAi(table, u.getUserId())) {
			return;
		}
		if (table.getDdz().getLastHand() == null) {
			DdzPlayService.autoPlaySmallest(table, u.getUserId());
		} else {
			DdzPlayService.apply(table, u.getUserId(),
					proto.GameProto.OpInfo.newBuilder().setChoice(proto.ConstProto.Operation.PASS).build());
		}
	}
}
