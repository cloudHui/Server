package game.manager.table.state;

import game.manager.table.Table;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * @author admin
 * @className IdleShowCard
 * @description
 * @createDate 2025/10/20 16:57
 */
@ProcessEnum(TableState.IDLE_SHOW_CARD)
public class IdleShowCard implements TableHandle {

	@Override
	public boolean handleState(Table table) {

		return false;
	}

	@Override
	public void overTime(Table table) {

	}
}
