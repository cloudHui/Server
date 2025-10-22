package game.manager.table.state;

import game.manager.table.Table;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * @author admin
 * @className TableDis
 * @description
 * @createDate 2025/10/20 16:57
 */
@ProcessEnum(TableState.TABLE_OVER)
public class TableDis extends AbstractTableHandle {

	@Override
	public boolean onTiming(Table table) {
		return true;
	}

	@Override
	protected void overTime(Table table) {

	}
}
