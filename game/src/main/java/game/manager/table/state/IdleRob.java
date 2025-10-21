package game.manager.table.state;

import java.util.Map;

import game.manager.table.Table;
import game.manager.table.TableUser;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import proto.ConstProto;
import proto.GameProto;
import utils.other.RandomUtils;

/**
 * @author admin
 * @className IdleRob
 * @description
 * @createDate 2025/10/20 16:57
 */
@ProcessEnum(TableState.IDLE_ROB)
public class IdleRob implements TableHandle {

	@Override
	public boolean handleState(Table table) {
		int firstRob = table.getBanner().getFirstRandomRobSeat();
		if (firstRob == -1) {
			firstRob = RandomUtils.randomRange(table.getUsers().size());
			table.getBanner().setFirstRandomRobSeat(firstRob);
			table.getOp().setLastOpSeat(table.getOp().getCurrOpSeat());
			table.getOp().setCurrOpSeat(firstRob);
		} else {
			table.getOp().moveToNextOp();
		}

		GameProto.NotOperation not = builderRobBannerOp(table.getOp().getCurrOpSeat(),
				firstRob != -1 ? new ConstProto.Operation[] { ConstProto.Operation.ROB, ConstProto.Operation.NOT_ROB }
						: new ConstProto.Operation[] { ConstProto.Operation.CALL, ConstProto.Operation.NOT_CALL });
		for (Map.Entry<Integer, TableUser> entry : table.getSeatUsers().entrySet()) {
			entry.getValue().sendRoleMessage(not, GMsg.NOT_OP, table.getTableId());
		}
		return false;
	}

	@Override
	public void overTime(Table table) {
	}

	/**
	 * 构造抢地主操作通知消息
	 *
	 * @return 构造消息
	 */
	private GameProto.NotOperation builderRobBannerOp(int seat, ConstProto.Operation[] ops) {
		GameProto.NotOperation.Builder builder = GameProto.NotOperation.newBuilder();
		builder.setWait(TableState.IDLE_ROB.getOverTime());
		builder.setOpSeat(seat);
		for (ConstProto.Operation op : ops) {
			builder.addChoice(GameProto.OpInfo.newBuilder()
					.setChoice(op)
					.build());
		}
		return builder.build();
	}
}
