package game.manager.table.state;

import game.manager.table.Table;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * @author admin
 * @className DefaultHandle
 * @description
 * @createDate 2025/10/22 14:18
 */
@ProcessEnum({ TableState.START_ANI, TableState.TABLE_OVER })
public class DefaultHandle extends AbstractTableHandle {

	@Override
	protected void overTime(Table table) {
		table.upNextState();
	}
}
