package game.manager.table.state;

import game.manager.table.MjTable;
import game.manager.table.Table;
import game.manager.table.mj.MjPlayService;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * 麻将摸牌阶段：超时自动摸牌，然后进入出牌阶段。
 * 摸牌后检查自摸胡。
 */
@ProcessEnum(TableState.MJ_PLAY)
public class MjPlay extends AbstractTableHandle {

	@Override
	public void overTime(Table table) {
		MjTable mjTable = (MjTable) table;
		if (mjTable.getMjTilePool() == null) {
			return;
		}
		// 牌墙空了 → 流局
		if (mjTable.getMjTilePool().remaining() <= 0) {
			MjPlayService.finishGame(mjTable, "牌墙已空(流局)");
			return;
		}
		// 摸牌
		int drawnTile = MjPlayService.drawTile(mjTable);
		if (drawnTile < 0) {
			MjPlayService.finishGame(mjTable, "摸牌失败");
			return;
		}

		// 检查自摸胡
		if (MjPlayService.checkZiMo(mjTable, drawnTile)) {
			return; // 已胡牌, 结束
		}

		// 进入出牌阶段(由MjDiscard发送出牌选项)
		long now = System.currentTimeMillis();
		table.upNextStateWithTime(TableState.MJ_DISCARD, now);
	}
}
