package game.manager.table.state;

import game.manager.table.Table;
import game.manager.table.ddz.DdzBidService;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * 等待叫地主；超时视为「不叫」。
 * 
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 */
@ProcessEnum(TableState.IDLE_ROB)
public class IdleRob extends AbstractTableHandle {

	@Override
	public void overTime(Table table) {
		DdzBidService.onBidTimeout(table);
	}
}
