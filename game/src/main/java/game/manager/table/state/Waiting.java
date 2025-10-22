package game.manager.table.state;

import game.manager.table.Table;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * @author admin
 * @className Waiting
 * @description
 * @createDate 2025/10/20 16:57
 */
@ProcessEnum(TableState.WAITING)
public class Waiting extends AbstractTableHandle {

	@Override
	public boolean onTiming(Table table) {
		if (table.sitFull()) {
			table.upNextState();
			table.sendCard();
			return false;
		}
		return table.isEmpty();
	}
}
