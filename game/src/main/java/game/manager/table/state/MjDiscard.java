package game.manager.table.state;

import game.manager.table.MjTable;
import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.mj.MjDrawService;
import game.manager.table.mj.MjPlayService;
import game.manager.table.mj.MjExposedSet;
import game.manager.table.mj.MjTableContext;
import game.manager.table.mj.MjWinChecker;
import game.manager.table.mj.ai.MjSimpleAi;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import proto.ConstProto;
import proto.GameProto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * 麻将出牌阶段：等待玩家出牌。
 * 首次进入时发送出牌提示(NotOperation)，超时自动出刚摸到的牌。
 * 支持: 出牌、暗杠、补杠
 */
@ProcessEnum(TableState.MJ_DISCARD)
public class MjDiscard extends AbstractTableHandle {

	private static final Logger logger = LoggerFactory.getLogger(MjDiscard.class);

	@Override
	public boolean onTiming(Table table) {
		MjTable mjTable = (MjTable) table;
		MjTableContext ctx = mjTable.getMjContext();
		if (!ctx.isDiscardPromptSent()) {
			sendDiscardPrompt(mjTable);
			ctx.setDiscardPromptSent(true);
		}
		return false;
	}

	/** 出牌超时处理：AI出牌或自动出牌，然后走公共afterDiscard流程 */
	@Override
	public void overTime(Table table) {
		MjTable mjTable = (MjTable) table;
		logger.info("麻将出牌超时, tableId: {}, seat: {}", table.getTableId(), table.getOp().getCurrOpSeat());

		if (table.getTableModel().getAutoPlay() == 0) {
			MjPlayService.afterDiscard(mjTable);
			return;
		}

		MjTableContext ctx = mjTable.getMjContext();
		int aiLevel = ctx.getAiLevel();
		if (aiLevel >= 0) {
			int seat = table.getOp().getCurrOpSeat();
			TableUser user = table.getSeatUser(seat);
			if (user != null) {
				int aiTile = MjSimpleAi.decideDiscard(mjTable, user, ctx.getDrawnTile());
				if (aiTile > 0) {
					GameProto.OpInfo op = GameProto.OpInfo.newBuilder()
							.setChoice(ConstProto.Operation.DISCARD)
							.addOpCards(GameProto.CardInfo.newBuilder()
									.addCards(GameProto.Card.newBuilder().setValue(aiTile).build())
									.build())
							.build();
					if (MjPlayService.applyDiscard(mjTable, user.getUserId(), op)) {
						MjPlayService.afterDiscard(mjTable);
						return;
					}
				}
			}
		}

		MjDrawService.autoDiscard(mjTable);
		MjPlayService.afterDiscard(mjTable);
	}

	private void sendDiscardPrompt(MjTable table) {
		int seat = table.getOp().getCurrOpSeat();
		TableUser user = table.getSeatUser(seat);
		if (user == null) return;

		table.getOp().clearChoiceMap();

		GameProto.NotOperation.Builder notBuilder = GameProto.NotOperation.newBuilder()
				.setWait(TableState.MJ_DISCARD.getOverTime())
				.setOpSeat(seat);

		// 出牌选项(总是可以出牌)
		GameProto.OpInfo discard = GameProto.OpInfo.newBuilder()
				.setChoice(ConstProto.Operation.DISCARD)
				.build();
		table.getOp().addPosOpInfo(seat, discard);
		notBuilder.addChoice(discard);

		// 检查暗杠(需配置允许)
		MjWinChecker winChecker = MjPlayService.createWinChecker(table);
		boolean allowAnGang = table.getTableModel().getAllowGangAn() != 0;
		List<Integer> anGangTiles = allowAnGang ? winChecker.getAnGangTiles(user.getCards()) : Collections.emptyList();
		for (int gangTileId : anGangTiles) {
			GameProto.OpInfo anGang = GameProto.OpInfo.newBuilder()
					.setChoice(ConstProto.Operation.MJ_GANG)
					.addOpCards(GameProto.CardInfo.newBuilder()
							.addCards(GameProto.Card.newBuilder().setValue(gangTileId).build())
							.build())
					.build();
			table.getOp().addPosOpInfo(seat, anGang);
			notBuilder.addChoice(anGang);
		}

		// 检查补杠(需配置允许)
		boolean allowBuGang = table.getTableModel().getAllowGangBu() != 0;
		MjTableContext ctx = table.getMjContext();
		for (MjExposedSet set : ctx.getExposedSets(seat)) {
			if (set.getType() == MjExposedSet.Type.PENG) {
				int pengTileId = set.getTileIds().get(0);
				if (winChecker.canBuGang(user.getCards(), ctx.getExposedSets(seat), pengTileId)) {
					GameProto.OpInfo buGang = GameProto.OpInfo.newBuilder()
							.setChoice(ConstProto.Operation.MJ_GANG)
							.addOpCards(GameProto.CardInfo.newBuilder()
									.addCards(GameProto.Card.newBuilder().setValue(pengTileId).build())
									.build())
							.build();
					table.getOp().addPosOpInfo(seat, buGang);
					notBuilder.addChoice(buGang);
				}
			}
		}

		TableUser sendUser = table.getSeatUser(seat);
		if (sendUser != null) {
			sendUser.sendRoleMessage(notBuilder.build(), GMsg.NOT_OP, table.getTableId());
		}
	}
}
