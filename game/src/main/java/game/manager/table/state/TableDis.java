package game.manager.table.state;

import game.Game;
import game.manager.table.Table;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * @author admin
 * @className TableDis
 * @description 牌局解散；停止桌子循环并从 TableManager 移除实例，避免泄漏。
 * @createDate 2025/10/20 16:57
 */
@ProcessEnum(TableState.TABLE_DIS)
public class TableDis extends AbstractTableHandle {

	@Override
	public boolean onTiming(Table table) {
		Game.getInstance().getTableManager().removeTable(table.getTableId());
		return true;
	}
}
