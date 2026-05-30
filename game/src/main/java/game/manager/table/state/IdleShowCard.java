package game.manager.table.state;

import game.manager.table.Table;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * @author admin
 * @className IdleShowCard
 * @description 空闲阶段：依赖 {@link AbstractTableHandle} 的超时逻辑切入
 *              {@link TableState#IDLE_SHOW_CARD}。
 * @createDate 2025/10/20 16:57
 */
@ProcessEnum(TableState.IDLE_SHOW_CARD)
public class IdleShowCard extends AbstractTableHandle {

	@Override
	public boolean onTiming(Table table) {
		// IDLE_SHOW_CARD 当前未在标准DDZ流程中使用
		// 仅用于地主明牌功能, 超时后自动进入下一状态
		return false;
	}

	@Override
	public void overTime(Table table) {
		// 超时自动跳过明牌, 进入出牌阶段
		table.upNextState(TableState.CARD);
	}
}
