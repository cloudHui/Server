package game.manager.table.state;

import game.manager.table.Table;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * @author admin
 * @className WaitHandle
 * @description
 * @createDate 2025/10/20 16:57
 */
@ProcessEnum(TableState.WAITING)
public class WaitHandle implements TableHandle {

	@Override
	public boolean handleState(Table table) {
		if (table.sitFull()) {
			table.setTableState(table.getTableState().getNext());
			table.sendCard();
			return false;
		}
		return table.isEmpty();
	}
}
