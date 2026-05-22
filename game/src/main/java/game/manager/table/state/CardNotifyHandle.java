package game.manager.table.state;

import game.manager.table.Table;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import proto.ConstProto;
import proto.GameProto;

/**
 * 出牌阶段：广播当前座位可操作项（出牌 / 过）。
 * 
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 */
@ProcessEnum(TableState.CARD)
public class CardNotifyHandle extends AbstractTableHandle {

	@Override
	public boolean onTiming(Table table) {
		int seat = table.getOp().getCurrOpSeat();
		table.getOp().clearChoiceMap();

		GameProto.OpInfo play = GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.PLAY).build();
		table.getOp().addPosOpInfo(seat, play);

		GameProto.NotOperation.Builder nb = GameProto.NotOperation.newBuilder()
				.setWait(TableState.IDLE_CARD.getOverTime())
				.setOpSeat(seat)
				.addChoice(play);

		if (table.getDdz().getLastHand() != null) {
			GameProto.OpInfo pass = GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.PASS).build();
			table.getOp().addPosOpInfo(seat, pass);
			nb.addChoice(pass);
		}

		table.sendTableMessage(nb.build(), GMsg.NOT_OP);
		table.upNextState();
		return false;
	}
}
