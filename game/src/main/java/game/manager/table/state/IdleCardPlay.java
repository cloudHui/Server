package game.manager.table.state;

import game.manager.table.DdzTable;
import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.ddz.DdzPlayService;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * 等待玩家出牌；超时自动处理（首家出最小单张，否则视为过）。
 * 
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 */
@ProcessEnum(TableState.IDLE_CARD)
public class IdleCardPlay extends AbstractTableHandle {

	@Override
	public void overTime(Table table) {
		DdzTable ddzTable = (DdzTable) table;
		int seat = table.getOp().getCurrOpSeat();
		TableUser u = table.getSeatUser(seat);
		if (u == null) {
			return;
		}
		if (DdzPlayService.autoPlayAi(ddzTable, u.getUserId())) {
			return;
		}
		if (ddzTable.getDdz().getLastHand() == null) {
			DdzPlayService.autoPlaySmallest(ddzTable, u.getUserId());
		} else {
			DdzPlayService.apply(ddzTable, u.getUserId(),
					proto.GameProto.OpInfo.newBuilder().setChoice(proto.ConstProto.Operation.PASS).build());
		}
	}
}
