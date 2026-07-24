package game.manager.table.state;

import game.Game;
import game.manager.table.MjTable;
import game.manager.table.Table;
import game.manager.table.ddz.DdzSettleService;
import game.manager.table.mj.MjSettleService;

/**
 * 总结算发送与散桌的公共入口，避免 TableOverBridge / ReqOpHandle 重复分支。
 */
public final class TableSettleSupport {

	private TableSettleSupport() {}

	public static void sendFinalGameResult(Table table) {
		if (table.getGameType() == 1) {
			MjSettleService.sendGameResult((MjTable) table);
		} else {
			DdzSettleService.sendGameResult(table);
		}
	}

	public static void sendFinalResultAndRemove(Table table) {
		if (table.isMultiRound()) {
			sendFinalGameResult(table);
		}
		Game.getInstance().getTableManager().removeTableAsync(table.getTableId());
	}
}
