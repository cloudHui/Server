package game.manager.table.state;

import game.manager.table.Table;
import game.manager.table.mj.MjPlayService;
import game.manager.table.mj.MjTableContext;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import proto.ConstProto;
import proto.GameProto;

/**
 * 麻将出牌阶段：等待玩家出牌。
 * 首次进入时发送出牌提示(NotOperation)，超时自动出刚摸到的牌。
 */
@ProcessEnum(TableState.MJ_DISCARD)
public class MjDiscard extends AbstractTableHandle {

	@Override
	public boolean onTiming(Table table) {
		MjTableContext ctx = table.getMjContext();
		if (!ctx.isDiscardPromptSent()) {
			sendDiscardPrompt(table);
			ctx.setDiscardPromptSent(true);
		}
		return false;
	}

	@Override
	public void overTime(Table table) {
		// 超时自动出牌(出刚摸到的牌)
		MjPlayService.autoDiscard(table);

		// 检查是否有人能碰/杠/胡
		if (!MjPlayService.checkClaim(table)) {
			// 无人响应，进入下一个玩家摸牌
			MjPlayService.nextPlayer(table);
			long now = System.currentTimeMillis();
			table.upNextStateWithTime(TableState.MJ_PLAY, now);
		}
		// 如果有人响应，checkClaim内部会处理状态转换(后续迭代)
	}

	private void sendDiscardPrompt(Table table) {
		int seat = table.getOp().getCurrOpSeat();

		table.getOp().clearChoiceMap();
		GameProto.OpInfo discard = GameProto.OpInfo.newBuilder()
				.setChoice(ConstProto.Operation.DISCARD)
				.build();
		table.getOp().addPosOpInfo(seat, discard);

		GameProto.NotOperation not = GameProto.NotOperation.newBuilder()
				.setWait(TableState.MJ_DISCARD.getOverTime())
				.setOpSeat(seat)
				.addChoice(discard)
				.build();
		table.sendTableMessage(not, GMsg.NOT_OP);
	}
}
