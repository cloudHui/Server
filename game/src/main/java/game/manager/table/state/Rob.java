package game.manager.table.state;

import java.util.concurrent.ThreadLocalRandom;

import game.manager.table.DdzTable;
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
		DdzTable ddzTable = (DdzTable) table;
		if (ddzTable.getBanner().isRobBroadcastDone()) {
			return false;
		}
		int seats = table.getTableModel().getSeatNum();

		if (!ddzTable.getBanner().isRobPhase()) {
			// 叫分阶段：仅首次选定首叫座位；后续轮次由 DdzBidService.moveToNextOp 推进，不得重置回首叫位。
			int seat = resolveCallOpSeat(table, ddzTable, seats);
			table.getOp().clearChoiceMap();

			GameProto.OpInfo notCall = GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.NOT_CALL).build();
			if (table.getTableModel().getGameSubType() == 1) {
				GameProto.OpInfo call = GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.CALL).build();
				table.getOp().addPosOpInfo(seat, call);
				table.sendTableMessage(GameProto.NotOperation.newBuilder().setWait(TableState.IDLE_ROB.getOverTime()).setOpSeat(seat).addChoice(notCall).addChoice(call).build(), GMsg.NOT_OP);
				ddzTable.getBanner().setRobBroadcastDone(true); table.upNextState(); return false;
			}
			table.getOp().addPosOpInfo(seat, notCall);
			GameProto.NotOperation.Builder notBuilder = GameProto.NotOperation.newBuilder()
					.setWait(TableState.IDLE_ROB.getOverTime())
					.setOpSeat(seat).addChoice(notCall);
			for (int score = 1; score <= 3; score++) {
				if (ddzTable.getBanner().isScoreAvailable(score)) {
					int choice = score == 1 ? ConstProto.Operation.CALL_SCORE_1_VALUE
							: score == 2 ? ConstProto.Operation.CALL_SCORE_2_VALUE : ConstProto.Operation.CALL_SCORE_3_VALUE;
					GameProto.OpInfo call = GameProto.OpInfo.newBuilder().setChoiceValue(choice).build();
					table.getOp().addPosOpInfo(seat, call);
					notBuilder.addChoice(call);
				}
			}
			table.sendTableMessage(notBuilder.build(), GMsg.NOT_OP);
		} else {
			int seat = ddzTable.getBanner().getCurrentRobSeat();
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

		ddzTable.getBanner().setRobBroadcastDone(true);
		table.upNextState();
		return false;
	}

	/**
	 * 解析叫分阶段当前操作座位：首次随机首叫，之后沿用已推进的 currOpSeat。
	 */
	private static int resolveCallOpSeat(Table table, DdzTable ddzTable, int seats) {
		int first = ddzTable.getBanner().getFirstRandomRobSeat();
		if (first < 0) {
			first = ThreadLocalRandom.current().nextInt(seats);
			ddzTable.getBanner().setFirstRandomRobSeat(first);
			table.getOp().setCurrOpSeat(first);
			return first;
		}
		int seat = table.getOp().getCurrOpSeat();
		if (seat < 0) {
			seat = first;
			table.getOp().setCurrOpSeat(seat);
		}
		return seat;
	}
}
