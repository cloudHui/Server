package game.manager.table.state;

import game.Game;
import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.mj.MjSettleService;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import proto.ConstProto;
import proto.GameProto;

/**
 * 牌局结束阶段
 */
@ProcessEnum(TableState.TABLE_OVER)
public class TableOverBridge extends AbstractTableHandle {

	@Override
	public boolean onTiming(Table table) {
		java.util.Set<GameProto.OpInfo> currChoice = table.getOp().getCurrChoice();
		if (currChoice != null && !currChoice.isEmpty()) return false;

		if (table.isMultiRound() && !table.isLastRound()) {
			sendPreparePrompt(table);
			if (table.getTableModel().getAutoNextRound() == 1) {
				for (int userId : table.getUsers().keySet()) table.addReady(userId);
			} else {
				// 机器人自动准备，避免卡在等人准备
				for (TableUser u : table.getSeatUsers().values()) {
					if (u.isRobot()) table.addReady(u.getUserId());
				}
			}
			table.getOp().setCurrOpSeat(-1); // 标记已发送
		}

		if (table.isMultiRound() && !table.isLastRound() && table.allReady()) {
			table.resetForNextRound();
			table.upNextState(TableState.WAITING);
			return false;
		}
		return false;
	}

	@Override
	protected void overTime(Table table) {
		if (table.isMultiRound()) {
			if (table.getGameType() == 1) {
				MjSettleService.sendGameResult(game.manager.table.MjTable.class.cast(table));
			} else {
				game.manager.table.ddz.DdzSettleService.sendGameResult(table);
			}
		}
		Game.getInstance().getTableManager().removeTable(table.getTableId());
	}

	private void sendPreparePrompt(Table table) {
		table.clearReadySet();
		for (TableUser user : table.getSeatUsers().values()) {
			GameProto.NotOperation not = GameProto.NotOperation.newBuilder()
					.setWait(TableState.TABLE_OVER.getOverTime())
					.setOpSeat(user.getSeated())
					.addChoice(GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.PREPARE).build())
					.build();
			user.sendRoleMessage(not, GMsg.NOT_OP, table.getTableId());
		}
	}
}
