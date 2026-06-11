package game.manager.table.state;

import game.manager.table.MjTable;
import game.manager.table.Table;
import game.manager.table.TableUser;
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

import java.util.Collections;
import java.util.List;

/**
 * 麻将出牌阶段：等待玩家出牌。
 * 首次进入时发送出牌提示(NotOperation)，超时自动出刚摸到的牌。
 * 支持: 出牌、暗杠、补杠
 */
@ProcessEnum(TableState.MJ_DISCARD)
public class MjDiscard extends AbstractTableHandle {

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

	@Override
	public void overTime(Table table) {
		MjTable mjTable = (MjTable) table;
		// autoPlay=0时不自动出牌, 等待玩家操作(超时pass)
		if (table.getTableModel().getAutoPlay() == 0) {
			// 不自动: 超时pass, 进入下一个玩家
			MjPlayService.nextPlayer(mjTable);
			long now = System.currentTimeMillis();
			table.upNextStateWithTime(TableState.MJ_PLAY, now);
			return;
		}

		// 有 AI 等级时使用 AI 决策出牌
		MjTableContext ctx = mjTable.getMjContext();
		int aiLevel = ctx.getAiLevel();
		if (aiLevel >= 0) {
			int seat = table.getOp().getCurrOpSeat();
			TableUser user = table.getSeatUser(seat);
			if (user != null) {
				int aiTile = MjSimpleAi.decideDiscard(mjTable, user, ctx.getDrawnTile());
				if (aiTile > 0) {
					// 用 AI 选出的牌代替默认的"摸什么打什么"
					GameProto.OpInfo op = GameProto.OpInfo.newBuilder()
							.setChoice(ConstProto.Operation.DISCARD)
							.addOpCards(GameProto.CardInfo.newBuilder()
									.addCards(GameProto.Card.newBuilder().setValue(aiTile).build())
									.build())
							.build();
					boolean success = MjPlayService.applyDiscard(mjTable, user.getUserId(), op);
					if (success) {
						if (!MjPlayService.checkClaim(mjTable)) {
							MjPlayService.nextPlayer(mjTable);
							long now = System.currentTimeMillis();
							table.upNextStateWithTime(TableState.MJ_PLAY, now);
						}
						return;
					}
				}
			}
		}

		// fallback: 超时自动出牌(出刚摸到的牌)
		MjPlayService.autoDiscard(mjTable);

		// 检查是否有人能碰/杠/胡
		if (!MjPlayService.checkClaim(mjTable)) {
			// 无人响应，进入下一个玩家摸牌
			MjPlayService.nextPlayer(mjTable);
			long now = System.currentTimeMillis();
			table.upNextStateWithTime(TableState.MJ_PLAY, now);
		}
		// 如果有人响应，checkClaim内部会进入MJ_CLAIM状态
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
