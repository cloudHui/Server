package game.manager.table.mj;

import game.manager.table.MjTable;
import game.manager.table.TableUser;
import game.manager.table.card.mj.MjConst;
import game.manager.table.card.mj.MjTilePool;
import game.manager.table.cards.Card;
import game.manager.table.replay.MjReplayRecorder;
import msg.registor.message.GMsg;
import proto.ConstProto;
import proto.GameProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 麻将摸牌服务
 * 处理摸牌、自摸检测、超时自动出牌、赖子翻牌
 */
public class MjDrawService {

	private static final Logger logger = LoggerFactory.getLogger(MjDrawService.class);

	private MjDrawService() {}

	// ======================== 摸牌 ========================

	/** 当前玩家摸牌，返回摸到的牌ID，-1表示牌墙已空 */
	public static int drawTile(MjTable table) {
		MjTilePool tilePool = table.getMjTilePool();
		if (tilePool == null || tilePool.remaining() <= 0) {
			logger.warn("牌墙已空, table: {}", table.getTableId());
			return -1;
		}
		int seat = table.getOp().getCurrOpSeat();
		TableUser user = table.getSeatUser(seat);
		if (user == null) {
			logger.error("摸牌时玩家不存在, table: {}, seat: {}", table.getTableId(), seat);
			return -1;
		}
		MjTableContext ctx = table.getMjContext();
		if (tilePool.remaining() == 1) ctx.setHaiDi(true);

		int tileId = tilePool.drawTile();
		user.addCards(new Card(tileId));
		ctx.setDrawnTile(tileId);
		ctx.setTileDrawn(true);
		tilePool.sendHandNotice(table.getSeatUsers());

		MjReplayRecorder replay = (MjReplayRecorder) table.getReplayRecorder();
		if (replay != null) replay.recordDraw(seat, tileId);

		logger.info("麻将摸牌, table: {}, seat: {}, tile: {}, 剩余: {}",
				table.getTableId(), seat, tileId, tilePool.remaining());
		return tileId;
	}

	// ======================== 自摸胡 ========================

	/** 检查当前玩家是否自摸胡，是则处理胡牌并返回true */
	public static boolean checkZiMo(MjTable table, int drawnTile) {
		int seat = table.getOp().getCurrOpSeat();
		TableUser user = table.getSeatUser(seat);
		if (user == null) return false;

		MjTableContext ctx = table.getMjContext();
		MjWinChecker winChecker = MjPlayService.createWinChecker(table);
		List<Card> handTiles = user.getCards();
		List<MjExposedSet> exposedSets = ctx.getExposedSets(seat);

		if (winChecker.canWin(handTiles, exposedSets, drawnTile)) {
			// 荆门开口笑检查: 天胡(庄家第一轮且无副露)可以不开口
			if (table.getTableModel().getGameSubType() == 1) {
				boolean isTianHu = ctx.getDealerSeat() == seat && exposedSets.isEmpty()
						&& handTiles.size() == MjConst.INIT_HAND + 1;
				if (!isTianHu && !ctx.hasOpened(seat) && exposedSets.isEmpty()) {
					return false;
				}
			}
			processZiMo(table, seat, drawnTile);
			return true;
		}
		return false;
	}

	/** 处理自摸胡牌（包内可见，供 MjClaimService 调用） */
	static void processZiMo(MjTable table, int seat, int drawnTile) {
		TableUser user = table.getSeatUser(seat);
		MjTableContext ctx = table.getMjContext();

		MjWinResult winResult = new MjWinResult();
		winResult.setWinnerId(seat);
		winResult.setWinTile(drawnTile);
		winResult.setZiMo(true);
		winResult.setHandTiles(new ArrayList<>(user.getCards()));
		winResult.setExposedSets(new ArrayList<>(ctx.getExposedSets(seat)));
		winResult.setGangShangKaiHua(ctx.isGangShangKaiHua());
		winResult.setHaiDi(ctx.isHaiDi());

		MjReplayRecorder replay = (MjReplayRecorder) table.getReplayRecorder();
		if (replay != null) replay.recordHu(seat, drawnTile, true);

		MjClaimService.clearClaimState(table);
		MjSettleService.finishGameWithWin(table, winResult);
	}

	// ======================== 超时自动出牌 ========================

	/** 超时自动出牌(出刚摸到的牌) */
	public static void autoDiscard(MjTable table) {
		logger.info("麻将超时自动出牌触发, tableId: {}", table.getTableId());
		int seat = table.getOp().getCurrOpSeat();
		TableUser user = table.getSeatUser(seat);
		if (user == null) return;

		MjTableContext ctx = table.getMjContext();
		int tileId = ctx.getDrawnTile();
		if (tileId == 0) {
			List<Card> cards = user.getCards();
			if (!cards.isEmpty()) {
				tileId = cards.get(cards.size() - 1).getId();
			} else {
				return;
			}
		}

		boolean removed = user.removeCardsByProtoIds(Collections.singletonList(tileId));
		if (!removed) {
			logger.error("自动出牌失败, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
			return;
		}

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
		if (replay != null) replay.recordAutoDiscard(seat, tileId);

		logger.info("麻将超时自动出牌, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
	}

	// ======================== 赖子翻牌 ========================

	/** 翻牌确定赖子(荆门麻将) */
	public static void flipLaiZi(MjTable table) {
		MjTilePool tilePool = table.getMjTilePool();
		if (tilePool == null || tilePool.remaining() <= 0) return;

		int flipTile = tilePool.drawTile();
		MjTableContext ctx = table.getMjContext();
		ctx.setLaiZiFlipTile(flipTile);
		int laiZi = nextTile(flipTile);
		ctx.setLaiZiTileId(laiZi);

		GameProto.NotMjState not = GameProto.NotMjState.newBuilder()
				.setOpSeat(-1).setTileId(flipTile)
				.setAction(ConstProto.Operation.DRAW)
				.setWallLeft(tilePool.remaining()).build();
		table.sendTableMessage(not, GMsg.MJ_TILE_NOT);

		logger.info("麻将翻牌确定赖子, table: {}, flipTile: {}, laiZi: {}", table.getTableId(), flipTile, laiZi);
	}

	/** 获取下一张牌(赖子计算) */
	private static int nextTile(int tileId) {
		int suit = MjConst.suitOf(tileId);
		int value = MjConst.valueOf(tileId);
		if (suit <= MjConst.SUIT_TONG) {
			return MjConst.encode(suit, value >= 9 ? 1 : value + 1);
		} else if (suit == MjConst.SUIT_FENG) {
			return MjConst.encode(suit, value >= 4 ? 1 : value + 1);
		} else {
			return MjConst.encode(suit, value >= 3 ? 1 : value + 1);
		}
	}
}
