package game.manager.table.state;

import game.manager.table.MjTable;
import game.manager.table.Table;
import game.manager.table.mj.MjDrawService;
import game.manager.table.mj.MjSettleService;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 麻将摸牌阶段：立即自动摸牌，然后进入出牌阶段。
 * 摸牌后检查自摸胡。超时仅作兜底。
 */
@ProcessEnum(TableState.MJ_PLAY)
public class MjPlay extends AbstractTableHandle {

	private static final Logger logger = LoggerFactory.getLogger(MjPlay.class);

	/** 主流程：进入状态立即摸牌 */
	@Override
	public boolean onTiming(Table table) {
		return doDrawTile(table);
	}

	/** 兜底：超时也执行摸牌 */
	@Override
	public void overTime(Table table) {
		logger.warn("麻将摸牌超时兜底触发, tableId: {}, seat: {}", table.getTableId(), table.getOp().getCurrOpSeat());
		doDrawTile(table);
	}

	/** 摸牌核心逻辑：牌墙空→流局，摸牌→检查自摸→进入出牌 */
	private boolean doDrawTile(Table table) {
		MjTable mjTable = (MjTable) table;
		if (mjTable.getMjTilePool() == null) return false;
		if (mjTable.getMjTilePool().remaining() <= 0) {
			MjSettleService.finishGame(mjTable, "牌墙已空(流局)");
			return true;
		}
		int drawnTile = MjDrawService.drawTile(mjTable);
		if (drawnTile < 0) {
			MjSettleService.finishGame(mjTable, "摸牌失败");
			return true;
		}
		if (MjDrawService.checkZiMo(mjTable, drawnTile)) return true;
		table.upNextStateWithTime(TableState.MJ_DISCARD, System.currentTimeMillis());
		return false;
	}
}
