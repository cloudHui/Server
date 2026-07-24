package game.manager.table.state;

import game.Game;
import game.manager.table.MjTable;
import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.ddz.DdzSettleService;
import game.manager.table.mj.MjSettleService;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import proto.ConstProto;
import proto.GameProto;

/**
 * 小结算阶段（TABLE_OVER）：展示约 15 秒后进入下一局或总结算散桌。
 * <p>
 * AbstractTableHandle 在 overTime&gt;0 时不会调用 onTiming，因此这里重写 handle，
 * 每 tick 处理准备提示，超时后再自动准备/散桌。
 */
@ProcessEnum(TableState.TABLE_OVER)
public class TableOverBridge extends AbstractTableHandle {

	@Override
	public boolean handle(Table table) {
		// 每 tick：下发准备提示；手动房机器人先准备；若已全员准备则可提前开下一局。
		processSettleTick(table);
		// 若 tick 内已切到 WAITING，勿再按 TABLE_OVER 超时逻辑处理。
		if (table.getTableState() != TableState.TABLE_OVER) {
			return false;
		}
		return super.handle(table);
	}

	@Override
	public boolean onTiming(Table table) {
		return false;
	}

	@Override
	protected void overTime(Table table) {
		// 15 秒小结算结束：自动开下一局（autoNextRound/机器人房），否则总结算并散桌。
		if (canContinueNextRound(table)) {
			autoReadyForNextRound(table);
			if (table.allReady()) {
				table.resetForNextRound();
				table.upNextState(TableState.WAITING);
				return;
			}
		}
		if (table.isMultiRound()) {
			if (table.getGameType() == 1) {
				MjSettleService.sendGameResult((MjTable) table);
			} else {
				DdzSettleService.sendGameResult(table);
			}
		}
		Game.getInstance().getTableManager().removeTableAsync(table.getTableId());
	}

	/** 小结算 tick：仅发一次准备提示；非自动房给机器人自动准备。 */
	private void processSettleTick(Table table) {
		if (!canContinueNextRound(table)) {
			return;
		}
		java.util.Set<GameProto.OpInfo> currChoice = table.getOp().getCurrChoice();
		if (currChoice == null || currChoice.isEmpty()) {
			sendPreparePrompt(table);
			if (table.getTableModel().getAutoNextRound() != 1) {
				// 非自动下一局：机器人自动准备，真人需手动点准备。
				for (TableUser u : table.getSeatUsers().values()) {
					if (u.isRobot()) {
						table.addReady(u.getUserId());
					}
				}
			}
			table.getOp().setCurrOpSeat(-1);
		}
		if (table.allReady()) {
			table.resetForNextRound();
			table.upNextState(TableState.WAITING);
		}
	}

	private static boolean canContinueNextRound(Table table) {
		return table.isMultiRound() && !table.isLastRound();
	}

	/** 超时自动准备：快速房全员准备；否则仅机器人补齐。 */
	private static void autoReadyForNextRound(Table table) {
		if (table.getTableModel().getAutoNextRound() == 1 || table.isRobotRoom()) {
			for (int userId : table.getUsers().keySet()) {
				table.addReady(userId);
			}
			return;
		}
		for (TableUser u : table.getSeatUsers().values()) {
			if (u.isRobot()) {
				table.addReady(u.getUserId());
			}
		}
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
