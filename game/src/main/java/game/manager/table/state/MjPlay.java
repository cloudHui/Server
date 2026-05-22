package game.manager.table.state;

import game.manager.table.Table;
import game.manager.table.mj.MjPlayService;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * 麻将摸牌阶段：超时自动摸牌，然后进入出牌阶段。
 */
@ProcessEnum(TableState.MJ_PLAY)
public class MjPlay extends AbstractTableHandle {

	@Override
	public void overTime(Table table) {
		if (table.getMjTilePool() == null) {
			return;
		}
		// 牌墙空了 → 流局
		if (table.getMjTilePool().remaining() <= 0) {
			MjPlayService.finishGame(table, "牌墙已空(流局)");
			return;
		}
		// 摸牌
		MjPlayService.drawTile(table);
		// 进入出牌阶段
		long now = System.currentTimeMillis();
		table.upNextStateWithTime(TableState.MJ_DISCARD, now);
	}
}
