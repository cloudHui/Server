package game.manager.table.state;

import game.manager.table.MjTable;
import game.manager.table.Table;
import game.manager.table.mj.MjPlayService;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * 麻将claim等待阶段: 出牌后等待其他玩家碰/杠/胡/过响应
 * 超时自动视为pass
 */
@ProcessEnum(TableState.MJ_CLAIM)
public class MjClaim extends AbstractTableHandle {

	@Override
	public void overTime(Table table) {
		MjTable mjTable = (MjTable) table;
		// 超时: 所有待响应的座位自动pass
		MjPlayService.timeoutClaim(mjTable);
	}
}
