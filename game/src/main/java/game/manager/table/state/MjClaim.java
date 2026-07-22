package game.manager.table.state;

import java.util.ArrayList;
import java.util.List;

import game.manager.table.MjTable;
import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.mj.MjClaimInfo;
import game.manager.table.mj.MjClaimService;
import game.manager.table.mj.MjTableContext;
import game.manager.table.mj.ai.MjSimpleAi;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GameProto;

/**
 * 麻将claim等待阶段: 出牌后等待其他玩家碰/杠/胡/过响应
 * 超时自动视为pass（有AI时使用AI决策）
 */
@ProcessEnum(TableState.MJ_CLAIM)
public class MjClaim extends AbstractTableHandle {

	private static final Logger logger = LoggerFactory.getLogger(MjClaim.class);

	@Override
	public boolean handle(Table table) {
		MjTableContext ctx = ((MjTable) table).getMjContext();
		boolean onlyRobotPending = true;
		for (int seat : ctx.getPendingClaimSeats()) {
			TableUser user = table.getSeatUser(seat);
			if (user != null && !user.isRobot()) {
				onlyRobotPending = false;
				break;
			}
		}
		if ((onlyRobotPending || table.isAutoPlayEnabled())
				&& System.currentTimeMillis() >= table.getStateStartTime() + 400) {
			overTime(table);
			return false;
		}
		return super.handle(table);
	}

	@Override
	public void overTime(Table table) {
		MjTable mjTable = (MjTable) table;
		MjTableContext ctx = mjTable.getMjContext();
		int aiLevel = ctx.getAiLevel();
		logger.info("麻将claim超时, tableId: {}, pendingSeats: {}, aiLevel: {}",
				table.getTableId(), ctx.getPendingClaimSeats(), aiLevel);

		if (aiLevel >= 0 || table.isAutoPlayEnabled() || hasRobotPending(table, ctx)) {
			// AI 决策：遍历每个待响应座位，用 AI 判断
			List<Integer> pending = new ArrayList<>(ctx.getPendingClaimSeats());
			for (int seat : pending) {
				TableUser user = table.getSeatUser(seat);
				if (user == null) {
					continue;
				}
				if (!user.isRobot() && !table.isAutoPlayEnabled() && aiLevel < 0) {
					continue;
				}
				MjClaimInfo claimInfo = MjClaimService.buildClaimInfo(mjTable, seat);
				if (claimInfo == null) {
					continue;
				}
				GameProto.OpInfo decision = MjSimpleAi.decideClaim(mjTable, user, claimInfo);
				// 执行 AI 决策（胡/碰/杠/吃/过）
				MjClaimService.applyClaim(mjTable, user.getUserId(), decision);
			}
			return;
		}

		// fallback: 超时全部自动pass
		MjClaimService.timeoutClaim(mjTable);
	}

	private static boolean hasRobotPending(Table table, MjTableContext ctx) {
		for (int seat : ctx.getPendingClaimSeats()) {
			TableUser user = table.getSeatUser(seat);
			if (user != null && user.isRobot()) return true;
		}
		return false;
	}
}
