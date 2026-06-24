package game.manager.table.state;

import game.manager.table.DdzTable;
import game.manager.table.Table;
import game.manager.table.ddz.DdzBidService;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger logger = LoggerFactory.getLogger(IdleRob.class);

	@Override
	public void overTime(Table table) {
		logger.info("叫分/抢地主超时, tableId: {}", table.getTableId());
		DdzTable ddzTable = (DdzTable) table;
		DdzBidService.onBidTimeout(ddzTable);
	}
}
