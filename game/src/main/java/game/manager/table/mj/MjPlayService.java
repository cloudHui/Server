package game.manager.table.mj;

import game.manager.table.MjTable;
import game.manager.table.TableUser;
import game.manager.table.cards.Card;
import game.manager.table.replay.MjReplayRecorder;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ConstProto;
import proto.GameProto;

import java.util.Collections;

/**
 * 麻将流程控制服务
 * 负责出牌处理、流程推进、WinChecker/Scoring工厂创建
 * 具体业务拆分到: MjDrawService(摸牌), MjClaimService(claim), MjGangService(杠), MjSettleService(结算)
 */
public class MjPlayService {

	private static final Logger logger = LoggerFactory.getLogger(MjPlayService.class);

	private MjPlayService() {}

	// ======================== 出牌 ========================

	/** 处理玩家出牌请求 */
	public static boolean applyDiscard(MjTable table, int userId, GameProto.OpInfo opInfo) {
		int seat = table.getOp().getCurrOpSeat();
		TableUser user = table.getSeatUser(seat);
		if (user == null || user.getUserId() != userId) {
			logger.warn("出牌操作座位不匹配, table: {}, seat: {}, userId: {}", table.getTableId(), seat, userId);
			return false;
		}
		if (opInfo.getOpCardsCount() == 0) {
			logger.warn("出牌未指定牌, table: {}, userId: {}", table.getTableId(), userId);
			return false;
		}

		int tileId = opInfo.getOpCards(0).getCards(0).getValue();
		boolean removed = user.removeCardsByProtoIds(Collections.singletonList(tileId));
		if (!removed) {
			logger.warn("出牌不在手牌中, table: {}, userId: {}, tile: {}", table.getTableId(), userId, tileId);
			return false;
		}

		MjTableContext ctx = table.getMjContext();
		ctx.setLastDiscardTile(tileId);
		ctx.setLastDiscardSeat(seat);
		ctx.addDiscard(seat, tileId);
		ctx.resetTurn();
		ctx.setGangShangKaiHua(false);

		GameProto.NotMjState not = GameProto.NotMjState.newBuilder()
				.setOpSeat(seat).setTileId(tileId)
				.setAction(ConstProto.Operation.DISCARD)
				.setWallLeft(table.getMjTilePool().remaining()).build();
		table.sendTableMessage(not, GMsg.MJ_TILE_NOT);

		MjReplayRecorder replay = (MjReplayRecorder) table.getReplayRecorder();
		if (replay != null) replay.recordDiscard(seat, tileId);

		logger.info("麻将出牌, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
		return true;
	}

	// ======================== 流程控制 ========================

	/** 出牌后的公共流程：检测claim，无人响应则进入下一个玩家摸牌 */
	public static void afterDiscard(MjTable table) {
		if (!MjClaimService.checkClaim(table)) {
			nextPlayer(table);
			table.upNextStateWithTime(TableState.MJ_PLAY, System.currentTimeMillis());
		}
	}

	/** 移动到下一个玩家 */
	public static void nextPlayer(MjTable table) {
		int currSeat = table.getOp().getCurrOpSeat();
		int nextSeat = table.nextSeat(currSeat);
		table.getOp().setCurrOpSeat(nextSeat);
		table.getMjContext().resetTurn();
		table.getMjContext().setGangShangKaiHua(false);
	}

	// ======================== 工厂方法 ========================

	/** 根据桌子配置创建WinChecker */
	public static MjWinChecker createWinChecker(MjTable table) {
		int subType = table.getTableModel().getGameSubType();
		MjTableContext ctx = table.getMjContext();
		boolean allowSevenPairs = table.getTableModel().getAllowSevenPairs() != 0;

		switch (subType) {
			case 1: // 荆门
				return new JmWinChecker(ctx.getLaiZiTileId(), allowSevenPairs);
			case 2: // 卡五星
				return new KwWinChecker(new int[]{1, 2}, allowSevenPairs, true);
			default:
				return new MjWinChecker(allowSevenPairs);
		}
	}

	/** 根据桌子配置创建Scoring */
	public static MjScoring createScoring(MjTable table) {
		int subType = table.getTableModel().getGameSubType();
		switch (subType) {
			case 1: // 荆门
				return new JmMjScoring();
			case 2: // 卡五星
				MjWinChecker checker = createWinChecker(table);
				if (checker instanceof KwWinChecker) {
					return new KwMjScoring((KwWinChecker) checker);
				}
				logger.error("createScoring: 卡五星checker类型不匹配, tableId: {}", table.getTableId());
				return new JmMjScoring();
			default:
				return new JmMjScoring();
		}
	}
}
