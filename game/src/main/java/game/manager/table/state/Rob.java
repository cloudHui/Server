package game.manager.table.state;

import java.util.concurrent.ThreadLocalRandom;

import game.manager.table.Table;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import proto.ConstProto;
import proto.GameProto;

/**
 * 广播叫分或抢地主选项，并进入 {@link TableState#IDLE_ROB}。
 * 
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 */
@ProcessEnum(TableState.ROB)
public class Rob extends AbstractTableHandle {

	@Override
	public boolean onTiming(Table table) {
		if (table.getBanner().isRobBroadcastDone()) {
			return false;
		}
		int seats = table.getTableModel().getSeatNum();

		if (!table.getBanner().isRobPhase()) {
			int first = table.getBanner().getFirstRandomRobSeat();
			if (first < 0) {
				first = ThreadLocalRandom.current().nextInt(seats);
				table.getBanner().setFirstRandomRobSeat(first);
			}
			table.getOp().clearChoiceMap();
			table.getOp().setCurrOpSeat(first);

			GameProto.OpInfo notCall = GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.NOT_CALL).build();
			GameProto.OpInfo s1 = GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.CALL_SCORE_1).build();
			GameProto.OpInfo s2 = GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.CALL_SCORE_2).build();
			GameProto.OpInfo s3 = GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.CALL_SCORE_3).build();
			table.getOp().addPosOpInfo(first, notCall);
			table.getOp().addPosOpInfo(first, s1);
			table.getOp().addPosOpInfo(first, s2);
			table.getOp().addPosOpInfo(first, s3);

			GameProto.NotOperation not = GameProto.NotOperation.newBuilder()
					.setWait(TableState.IDLE_ROB.getOverTime())
					.setOpSeat(first)
					.addChoice(notCall)
					.addChoice(s1)
					.addChoice(s2)
					.addChoice(s3)
					.build();
			table.sendTableMessage(not, GMsg.NOT_OP);
		} else {
			int seat = table.getBanner().getCurrentRobSeat();
			if (seat < 0) {
				return false;
			}
			table.getOp().clearChoiceMap();
			table.getOp().setCurrOpSeat(seat);

			GameProto.OpInfo rob = GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.ROB).build();
			GameProto.OpInfo notRob = GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.NOT_ROB).build();
			table.getOp().addPosOpInfo(seat, rob);
			table.getOp().addPosOpInfo(seat, notRob);

			GameProto.NotOperation not = GameProto.NotOperation.newBuilder()
					.setWait(TableState.IDLE_ROB.getOverTime())
					.setOpSeat(seat)
					.addChoice(rob)
					.addChoice(notRob)
					.build();
			table.sendTableMessage(not, GMsg.NOT_OP);
		}

		table.getBanner().setRobBroadcastDone(true);
		table.upNextState();
		return false;
	}
}
