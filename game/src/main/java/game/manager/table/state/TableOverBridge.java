package game.manager.table.state;

import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * 牌局结束展示阶段：依赖 {@link AbstractTableHandle} 的超时逻辑切入 {@link TableState#TABLE_DIS}。
 */
@ProcessEnum(TableState.TABLE_OVER)
public class TableOverBridge extends AbstractTableHandle {
}
