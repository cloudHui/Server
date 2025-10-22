package game.manager.table.state;

import game.manager.table.Table;
import game.manager.table.TableUser;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ConstProto;
import proto.GameProto;

/**
 * @author admin
 * @className IdleRob
 * @description
 * @createDate 2025/10/20 16:57
 */
@ProcessEnum(TableState.IDLE_ROB)
public class IdleRob extends AbstractTableHandle {

	private static final Logger logger = LoggerFactory.getLogger(IdleRob.class);

	@Override
	public void overTime(Table table) {
		int currOpSeat = table.getOp().getCurrOpSeat();
		ConstProto.Operation operation = table.getBanner().getFirstRobSeat() != -1 ? ConstProto.Operation.NOT_ROB : ConstProto.Operation.NOT_CALL;
		TableUser seatUser = table.getSeatUser(currOpSeat);
		if (seatUser == null) {
			logger.error("table:{} seat:{} IdleRob role null", table.getTableId(), currOpSeat);
			return;
		}

		int nextSeat = table.nextSeat(currOpSeat);
		TableUser nextSeatUser = table.getSeatUser(nextSeat);
		if (nextSeatUser == null) {
			logger.error("table:{} seat:{} IdleRob nextSeatUser null", table.getTableId(), nextSeat);
			return;
		}
		//Todo 好像得判断是否到第一个人了 还没超过次数就重新发拍否则直接给第一个人
		

		table.sendTableMessage(GameProto.AckOp.newBuilder()
				.setOpId(seatUser.getUserId())
				.setOp(GameProto.OpInfo.newBuilder()
						.setChoice(operation)
						.build())
				.build(), GMsg.ACK_OP);
		table.getOp().moveToNextOp();
		table.upNextState();
	}
}
