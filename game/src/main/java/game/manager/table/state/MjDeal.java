package game.manager.table.state;

import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

/**
 * 麻将发牌展示阶段：发牌已在 Waiting 完成，本状态仅等待 overTime 后进入摸牌。
 * 超时切下一状态由 {@link AbstractTableHandle} 根据 {@link TableState#MJ_DEAL} 的 next 处理。
 */
@ProcessEnum(TableState.MJ_DEAL)
public class MjDeal extends AbstractTableHandle {
}
