package game.manager.table.state;

import game.manager.table.Table;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * @author admin
 * @className DefaultHandle
 * @description 默认阶段：依赖 {@link AbstractTableHandle} 的超时逻辑切入
 *              {@link TableState#START_ANI}。
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 * @createDate 2025/10/22 14:18
 */
@ProcessEnum(TableState.START_ANI)
public class DefaultHandle extends AbstractTableHandle {

	@Override
	protected void overTime(Table table) {
		table.upNextState();
	}
}
