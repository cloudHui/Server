package game.manager.table.mj;

import com.google.protobuf.ByteString;

import game.manager.table.MjTable;
import game.manager.table.TableUser;
import game.manager.table.card.mj.MjConst;
import game.manager.table.card.mj.MjTilePool;
import game.manager.table.cards.Card;
import game.manager.table.replay.ReplayRecorder;
import model.tablemodel.TableModel;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ConstProto;
import proto.GameProto;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 麻将出牌服务
 * 处理摸牌、出牌、碰/杠/吃/胡检测、claim流程、结算
 */
public class MjPlayService {

	private static final Logger logger = LoggerFactory.getLogger(MjPlayService.class);

	// ======================== 摸牌 ========================

	/**
	 * 当前玩家摸牌
	 *
	 * @return 摸到的牌ID，-1表示牌墙已空
	 */
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

		// 海底标记: 摸之前检查是否是最后一张
		MjTableContext ctx = table.getMjContext();
		if (tilePool.remaining() == 1) {
			ctx.setHaiDi(true);
		}

		int tileId = tilePool.drawTile();
		user.addCards(new Card(tileId));
		ctx.setDrawnTile(tileId);
		ctx.setTileDrawn(true);

		// 通知手牌
		tilePool.sendHandNotice(table.getSeatUsers());

		// 回放记录
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay != null) {
			replay.recordDraw(seat, tileId);
		}

		logger.info("麻将摸牌, table: {}, seat: {}, tile: {}, 剩余: {}",
				table.getTableId(), seat, tileId, tilePool.remaining());
		return tileId;
	}

	// ======================== 出牌 ========================

	/**
	 * 处理玩家出牌请求
	 */
	public static boolean applyDiscard(MjTable table, int userId, GameProto.OpInfo opInfo) {
		int seat = table.getOp().getCurrOpSeat();
		TableUser user = table.getSeatUser(seat);
		if (user == null || user.getUserId() != userId) {
			logger.warn("出牌操作座位不匹配, table: {}, seat: {}, userId: {}",
					table.getTableId(), seat, userId);
			return false;
		}

		if (opInfo.getOpCardsCount() == 0) {
			logger.warn("出牌未指定牌, table: {}, userId: {}", table.getTableId(), userId);
			return false;
		}

		int tileId = opInfo.getOpCards(0).getCards(0).getValue();

		// 从手牌中移除
		boolean removed = user.removeCardsByProtoIds(Collections.singletonList(tileId));
		if (!removed) {
			logger.warn("出牌不在手牌中, table: {}, userId: {}, tile: {}",
					table.getTableId(), userId, tileId);
			return false;
		}

		// 更新上下文
		MjTableContext ctx = table.getMjContext();
		ctx.setLastDiscardTile(tileId);
		ctx.setLastDiscardSeat(seat);
		ctx.addDiscard(seat, tileId);
		ctx.resetTurn();
		ctx.setGangShangKaiHua(false); // 清除杠花标记

		// 广播出牌通知
		GameProto.NotMjState not = GameProto.NotMjState.newBuilder()
				.setOpSeat(seat)
				.setTileId(tileId)
				.setAction(GameProto.MjAction.MJ_DISCARD_TILE)
				.setWallLeft(table.getMjTilePool().remaining())
				.build();
		table.sendTableMessage(not, GMsg.MJ_TILE_NOT);

		// 回放记录
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay != null) {
			replay.recordDiscard(seat, tileId);
		}

		logger.info("麻将出牌, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
		return true;
	}

	/**
	 * 超时自动出牌(出刚摸到的牌)
	 */
	public static void autoDiscard(MjTable table) {
		int seat = table.getOp().getCurrOpSeat();
		TableUser user = table.getSeatUser(seat);
		if (user == null) {
			return;
		}

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
				.setOpSeat(seat)
				.setTileId(tileId)
				.setAction(GameProto.MjAction.MJ_DISCARD_TILE)
				.setWallLeft(table.getMjTilePool().remaining())
				.build();
		table.sendTableMessage(not, GMsg.MJ_TILE_NOT);

		// 回放记录
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay != null) {
			replay.recordAutoDiscard(seat, tileId);
		}

		logger.info("麻将超时自动出牌, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
	}

	// ======================== Claim检测 ========================

	/**
	 * 出牌后检测其他玩家碰/杠/胡/吃机会
	 * 按优先级: 胡 > 杠 > 碰 > 吃
	 * 有claim时进入MJ_CLAIM状态等待响应
	 *
	 * @return true=有人有claim(进入claim状态), false=无人响应(继续下一个玩家)
	 */
	/**
	 * 为指定座位构建 claim 信息（供 AI 决策使用）
	 */
	public static MjClaimInfo buildClaimInfo(MjTable table, int seat) {
		MjTableContext ctx = table.getMjContext();
		int tileId = ctx.getClaimTileId();
		if (tileId == 0) {
			return null;
		}
		TableUser user = table.getSeatUser(seat);
		if (user == null) {
			return null;
		}
		MjWinChecker winChecker = createWinChecker(table);
		List<Card> handTiles = user.getCards();
		List<MjExposedSet> exposedSets = ctx.getExposedSets(seat);

		boolean canHu = false, canGang = false, canPeng = false, canChi = false;
		int gangTileId = 0;
		List<int[]> chiCombos = new ArrayList<>();

		boolean allowHu = table.getTableModel().getAllowHu() != 0;
		if (allowHu) {
			List<Card> testHand = new ArrayList<>(handTiles);
			testHand.add(new Card(tileId));
			if (winChecker.canWin(testHand, exposedSets, tileId)) {
				canHu = true;
			}
		}
		if (table.getTableModel().getAllowPeng() != 0 && winChecker.canPeng(handTiles, tileId)) {
			canPeng = true;
		}
		if (table.getTableModel().getAllowGangMing() != 0 && winChecker.canMingGang(handTiles, tileId)) {
			canGang = true;
			gangTileId = tileId;
		}
		if (table.getTableModel().getAllowChi() == 1) {
			int fromSeat = ctx.getClaimFromSeat();
			int seatNum = table.getTableModel().getSeatNum();
			int prevSeat = (fromSeat + seatNum - 1) % seatNum;
			if (seat == prevSeat) {
				chiCombos = winChecker.getChiCombos(handTiles, tileId);
				canChi = !chiCombos.isEmpty();
			}
		}

		if (!canHu && !canGang && !canPeng && !canChi) {
			return null;
		}
		return new MjClaimInfo(seat, canHu, canGang, canPeng, canChi, gangTileId, chiCombos);
	}

	public static boolean checkClaim(MjTable table) {
		MjTableContext ctx = table.getMjContext();
		int tileId = ctx.getLastDiscardTile();
		int fromSeat = ctx.getLastDiscardSeat();

		if (tileId == 0) {
			return false;
		}

		int seatNum = table.getTableModel().getSeatNum();
		boolean allowChi = table.getTableModel().getAllowChi() == 1;
		boolean allowPeng = table.getTableModel().getAllowPeng() != 0;
		boolean allowGangMing = table.getTableModel().getAllowGangMing() != 0;
		boolean allowHu = table.getTableModel().getAllowHu() != 0;
		MjWinChecker winChecker = createWinChecker(table);

		List<MjClaimInfo> claims = new ArrayList<>();
		List<Integer> waitingSeats = new ArrayList<>();

		for (int i = 1; i < seatNum; i++) {
			int checkSeat = (fromSeat + i) % seatNum;
			TableUser user = table.getSeatUser(checkSeat);
			if (user == null) continue;

			List<Card> handTiles = user.getCards();
			List<MjExposedSet> exposedSets = ctx.getExposedSets(checkSeat);
			boolean hasOpened = ctx.hasOpened(checkSeat);

			boolean canHu = false;
			boolean canGang = false;
			boolean canPeng = false;
			boolean canChi = false;
			int gangTileId = 0;
			List<int[]> chiCombos = new ArrayList<>();

			// 胡检测
			if (allowHu) {
				List<Card> testHand = new ArrayList<>(handTiles);
				testHand.add(new Card(tileId));
				if (winChecker.canWin(testHand, exposedSets, tileId)) {
					canHu = true;
				}
			}

			// 碰检测
			if (allowPeng && winChecker.canPeng(handTiles, tileId)) {
				canPeng = true;
			}

			// 杠检测(明杠)
			if (allowGangMing && winChecker.canMingGang(handTiles, tileId)) {
				canGang = true;
				gangTileId = tileId;
			}

			// 吃检测(只有上家可以吃)
			if (allowChi) {
				int prevSeat = (fromSeat + seatNum - 1) % seatNum;
				if (checkSeat == prevSeat) {
					chiCombos = winChecker.getChiCombos(handTiles, tileId);
					canChi = !chiCombos.isEmpty();
				}
			}

			if (canHu || canGang || canPeng || canChi) {
				claims.add(new MjClaimInfo(checkSeat, canHu, canGang, canPeng, canChi, gangTileId, chiCombos));
				waitingSeats.add(checkSeat);
			}
		}

		if (claims.isEmpty()) {
			return false;
		}

		// 设置claim信息, 进入MJ_CLAIM状态
		ctx.setClaimInfo(tileId, fromSeat, waitingSeats);

		// 给每个有claim的座位发送可选操作
		for (MjClaimInfo claim : claims) {
			sendClaimOptions(table, claim);
		}

		// 进入MJ_CLAIM状态
		table.upNextState(TableState.MJ_CLAIM);

		logger.info("麻将claim检测, table: {}, tile: {}, claimSeats: {}",
				table.getTableId(), tileId, waitingSeats);
		return true;
	}

	/**
	 * 给单个座位发送claim可选操作
	 */
	private static void sendClaimOptions(MjTable table, MjClaimInfo claim) {
		GameProto.NotMjState.Builder notBuilder = GameProto.NotMjState.newBuilder()
				.setOpSeat(claim.getSeat())
				.setTileId(table.getMjContext().getClaimTileId())
				.setAction(GameProto.MjAction.MJ_DISCARD_TILE)
				.setWait(TableState.MJ_CLAIM.getOverTime())
				.setWallLeft(table.getMjTilePool().remaining());

		// 按优先级添加可选操作
		if (claim.isCanHu()) {
			notBuilder.addChoice(GameProto.OpInfo.newBuilder()
					.setChoice(ConstProto.Operation.MJ_HU).build());
		}
		if (claim.isCanGang()) {
			notBuilder.addChoice(GameProto.OpInfo.newBuilder()
					.setChoice(ConstProto.Operation.MJ_GANG).build());
		}
		if (claim.isCanPeng()) {
			notBuilder.addChoice(GameProto.OpInfo.newBuilder()
					.setChoice(ConstProto.Operation.MJ_PENG).build());
		}
		if (claim.isCanChi()) {
			// 每种吃的组合单独作为一个选项
			for (int[] combo : claim.getChiCombos()) {
				GameProto.OpInfo.Builder chiBuilder = GameProto.OpInfo.newBuilder()
						.setChoice(ConstProto.Operation.MJ_CHI);
				GameProto.CardInfo.Builder cardInfoBuilder = GameProto.CardInfo.newBuilder();
				for (int chiTile : combo) {
					cardInfoBuilder.addCards(GameProto.Card.newBuilder().setValue(chiTile).build());
				}
				chiBuilder.addOpCards(cardInfoBuilder.build());
				notBuilder.addChoice(chiBuilder.build());
			}
		}
		// 总是可以pass
		notBuilder.addChoice(GameProto.OpInfo.newBuilder()
				.setChoice(ConstProto.Operation.MJ_PASS).build());

		// 记录所有操作选项到Op管理器(用于验证)
		table.getOp().clearChoiceMap();
		for (GameProto.OpInfo choice : notBuilder.getChoiceList()) {
			table.getOp().addPosOpInfo(claim.getSeat(), choice);
		}

		TableUser user = table.getSeatUser(claim.getSeat());
		if (user != null) {
			user.sendRoleMessage(notBuilder.build(), GMsg.MJ_TILE_NOT, table.getTableId());
		}
	}

	// ======================== Claim响应处理 ========================

	/**
	 * 处理玩家的claim响应(碰/杠/胡/吃/过)
	 */
	public static boolean applyClaim(MjTable table, int userId, GameProto.OpInfo op) {
		MjTableContext ctx = table.getMjContext();
		int seat = -1;
		for (Map.Entry<Integer, TableUser> entry : table.getSeatUsers().entrySet()) {
			if (entry.getValue().getUserId() == userId) {
				seat = entry.getKey();
				break;
			}
		}
		if (seat < 0 || !ctx.getPendingClaimSeats().contains(seat)) {
			return false;
		}

		ConstProto.Operation choice = op.getChoice();
		int tileId = ctx.getClaimTileId();
		int fromSeat = ctx.getClaimFromSeat();

		switch (choice) {
			case MJ_HU:
				return processHu(table, seat, tileId, fromSeat, false);
			case MJ_GANG:
				return processClaimGang(table, seat, tileId, fromSeat);
			case MJ_PENG:
				return processPeng(table, seat, tileId, fromSeat);
			case MJ_CHI:
				return processChi(table, seat, tileId, fromSeat, op);
			case MJ_PASS:
				return processPass(table, seat);
			default:
				logger.warn("无效的claim操作, table: {}, userId: {}, choice: {}",
						table.getTableId(), userId, choice);
				return false;
		}
	}

	/**
	 * 清理claim状态并取消所有待响应的座位
	 */
	private static void clearClaimState(MjTable table) {
		MjTableContext ctx = table.getMjContext();
		ctx.getPendingClaimSeats().clear();
		ctx.setClaimInfo(0, 0, Collections.emptyList());
	}

	/**
	 * 处理胡牌
	 */
	private static boolean processHu(MjTable table, int seat, int tileId, int fromSeat, boolean qiangGang) {
		TableUser user = table.getSeatUser(seat);
		if (user == null) return false;

		MjTableContext ctx = table.getMjContext();

		// 点炮胡: 加入这张牌到手牌
		if (!qiangGang) {
			user.addCards(new Card(tileId));
		}

		// 构建胡牌结果
		MjWinResult winResult = new MjWinResult();
		winResult.setWinnerId(seat);
		winResult.setWinTile(tileId);
		winResult.setZiMo(false);
		winResult.setDianPao(true);
		winResult.setDianPaoSeat(fromSeat);
		winResult.setHandTiles(new ArrayList<>(user.getCards()));
		winResult.setExposedSets(new ArrayList<>(ctx.getExposedSets(seat)));
		winResult.setGangShangKaiHua(ctx.isGangShangKaiHua());
		winResult.setQiangGangHu(qiangGang);
		winResult.setHaiDi(ctx.isHaiDi());

		// 清理claim状态
		clearClaimState(table);

		// 回放记录
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay != null) {
			replay.recordHu(seat, tileId, false);
		}

		// 结算
		finishGameWithWin(table, winResult);
		return true;
	}

	/**
	 * 处理claim碰
	 */
	private static boolean processPeng(MjTable table, int seat, int tileId, int fromSeat) {
		TableUser user = table.getSeatUser(seat);
		if (user == null) return false;

		// 从手牌移除2张
		int removed = 0;
		Iterator<Card> it = user.getCards().iterator();
		while (it.hasNext() && removed < 2) {
			if (it.next().getId() == tileId) {
				it.remove();
				removed++;
			}
		}
		if (removed < 2) {
			logger.error("碰牌时手牌不足, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
			return false;
		}

		// 添加副露
		MjTableContext ctx = table.getMjContext();
		ctx.addExposedSet(seat, new MjExposedSet(MjExposedSet.Type.PENG,
				Arrays.asList(tileId, tileId, tileId), fromSeat));

		// 清理claim状态(碰后其他人不能再操作)
		clearClaimState(table);

		// 碰后该玩家出牌
		table.getOp().setCurrOpSeat(seat);
		ctx.resetTurn();

		// 广播碰操作 + 副露区同步
		broadcastMjAction(table, seat, tileId, GameProto.MjAction.MJ_PENG);
		syncExposedSets(table);

		// 回放记录
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay != null) {
			replay.recordPeng(seat, fromSeat, tileId);
		}

		// 进入出牌阶段
		table.upNextState(TableState.MJ_DISCARD);

		logger.info("麻将碰, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
		return true;
	}

	/**
	 * 处理claim明杠
	 */
	private static boolean processClaimGang(MjTable table, int seat, int tileId, int fromSeat) {
		TableUser user = table.getSeatUser(seat);
		if (user == null) return false;

		// 从手牌移除3张
		int removed = 0;
		Iterator<Card> it = user.getCards().iterator();
		while (it.hasNext() && removed < 3) {
			if (it.next().getId() == tileId) {
				it.remove();
				removed++;
			}
		}
		if (removed < 3) {
			logger.error("杠牌时手牌不足, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
			return false;
		}

		// 添加副露(明杠)
		MjTableContext ctx = table.getMjContext();
		ctx.addExposedSet(seat, new MjExposedSet(MjExposedSet.Type.MING_GANG,
				Arrays.asList(tileId, tileId, tileId, tileId), fromSeat));

		// 清理claim状态
		clearClaimState(table);

		// 杠后补牌
		table.getOp().setCurrOpSeat(seat);
		ctx.resetTurn();
		ctx.setGangShangKaiHua(true);

		// 广播杠操作 + 副露区同步
		broadcastMjAction(table, seat, tileId, GameProto.MjAction.MJ_GANG);
		syncExposedSets(table);

		// 回放记录
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay != null) {
			replay.recordMingGang(seat, fromSeat, tileId);
		}

		// 杠分即时结算
		settleGangScore(table, seat, MjExposedSet.Type.MING_GANG);

		// 补牌
		int drawnTile = drawTile(table);
		if (drawnTile >= 0) {
			// 检查杠上开花
			List<Card> handTiles = user.getCards();
			MjWinChecker winChecker = createWinChecker(table);
			if (winChecker.canWin(handTiles, ctx.getExposedSets(seat), drawnTile)) {
				processZiMo(table, seat, drawnTile);
				return true;
			}
			// 不能胡, 清除杠花标记, 进入出牌阶段
			ctx.setGangShangKaiHua(false);
			table.upNextState(TableState.MJ_DISCARD);
		} else {
			finishGame(table, "杠后牌墙已空");
		}

		logger.info("麻将明杠, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
		return true;
	}

	/**
	 * 处理claim吃
	 */
	private static boolean processChi(MjTable table, int seat, int tileId, int fromSeat, GameProto.OpInfo op) {
		TableUser user = table.getSeatUser(seat);
		if (user == null) return false;

		// 从opCards中获取吃的组合
		if (op.getOpCardsCount() == 0) {
			return false;
		}
		List<Integer> chiTileIds = new ArrayList<>();
		for (GameProto.Card c : op.getOpCards(0).getCardsList()) {
			chiTileIds.add(c.getValue());
		}
		chiTileIds.add(tileId); // 加入别人出的牌

		// 从手牌移除吃的牌(不含别人出的那张)
		for (int chiTile : chiTileIds) {
			if (chiTile == tileId) continue;
			boolean removed = user.removeCardsByProtoIds(Collections.singletonList(chiTile));
			if (!removed) {
				logger.error("吃牌时手牌不足, table: {}, seat: {}, tile: {}", table.getTableId(), seat, chiTile);
				return false;
			}
		}

		// 添加副露
		MjTableContext ctx = table.getMjContext();
		ctx.addExposedSet(seat, new MjExposedSet(MjExposedSet.Type.CHI, chiTileIds, fromSeat));

		// 清理claim状态
		clearClaimState(table);

		// 吃后该玩家出牌
		table.getOp().setCurrOpSeat(seat);
		ctx.resetTurn();

		// 广播吃操作 + 副露区同步
		broadcastMjAction(table, seat, tileId, GameProto.MjAction.MJ_CHI);
		syncExposedSets(table);

		// 回放记录
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay != null) {
			replay.recordChi(seat, fromSeat, chiTileIds, user.getCards().stream()
					.mapToInt(Card::getId).boxed().collect(Collectors.toList()));
		}

		// 进入出牌阶段
		table.upNextState(TableState.MJ_DISCARD);

		logger.info("麻将吃, table: {}, seat: {}, tiles: {}", table.getTableId(), seat, chiTileIds);
		return true;
	}

	/**
	 * 处理pass
	 * 优先级逻辑: 如果还有高优先级的claim等待, 不进入下一个玩家
	 */
	private static boolean processPass(MjTable table, int seat) {
		MjTableContext ctx = table.getMjContext();
		ctx.removeClaimSeat(seat);

		// 如果所有人都pass了, 进入下一个玩家
		if (!ctx.hasPendingClaims()) {
			nextPlayer(table);
			table.upNextState(TableState.MJ_PLAY);
		}
		// 否则继续等待其他人响应
		return true;
	}

	/**
	 * claim超时: 所有待响应座位自动pass
	 */
	public static void timeoutClaim(MjTable table) {
		MjTableContext ctx = table.getMjContext();
		List<Integer> pending = new ArrayList<>(ctx.getPendingClaimSeats());

		// 回放记录
		ReplayRecorder replay = table.getReplayRecorder();
		for (int seat : pending) {
			if (replay != null) {
				replay.recordAutoPass(seat);
			}
			ctx.removeClaimSeat(seat);
		}
		clearClaimState(table);
		nextPlayer(table);
		table.upNextState(TableState.MJ_PLAY);
	}

	// ======================== 自摸胡 ========================

	/**
	 * 处理自摸胡(摸牌后检查)
	 */
	public static boolean checkZiMo(MjTable table, int drawnTile) {
		int seat = table.getOp().getCurrOpSeat();
		TableUser user = table.getSeatUser(seat);
		if (user == null) return false;

		MjTableContext ctx = table.getMjContext();
		MjWinChecker winChecker = createWinChecker(table);
		List<Card> handTiles = user.getCards();
		List<MjExposedSet> exposedSets = ctx.getExposedSets(seat);
		int round = table.getTableModel().getGameSubType();

		if (winChecker.canWin(handTiles, exposedSets, drawnTile)) {
			// 荆门开口笑检查: 天胡(庄家第一轮且无副露)可以不开口
			if (round == 1) {
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

	/**
	 * 处理自摸胡
	 */
	private static void processZiMo(MjTable table, int seat, int drawnTile) {
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

		// 回放记录
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay != null) {
			replay.recordHu(seat, drawnTile, true);
		}

		clearClaimState(table);
		finishGameWithWin(table, winResult);
	}

	// ======================== 暗杠/补杠 ========================

	/**
	 * 玩家主动暗杠(出牌阶段选择)
	 * @param gangTileId 要暗杠的牌ID
	 * @return true=执行了暗杠
	 */
	public static boolean applyAnGang(MjTable table, int gangTileId) {
		int seat = table.getOp().getCurrOpSeat();
		TableUser user = table.getSeatUser(seat);
		if (user == null) return false;

		MjWinChecker winChecker = createWinChecker(table);
		List<Integer> gangTiles = winChecker.getAnGangTiles(user.getCards());

		if (!gangTiles.contains(gangTileId)) {
			return false;
		}

		// 从手牌移除4张
		int removed = 0;
		Iterator<Card> it = user.getCards().iterator();
		while (it.hasNext() && removed < 4) {
			if (it.next().getId() == gangTileId) {
				it.remove();
				removed++;
			}
		}

		MjTableContext ctx = table.getMjContext();
		ctx.addExposedSet(seat, new MjExposedSet(MjExposedSet.Type.AN_GANG,
				Arrays.asList(gangTileId, gangTileId, gangTileId, gangTileId), -1));

		// 广播 + 副露同步
		broadcastMjAction(table, seat, gangTileId, GameProto.MjAction.MJ_GANG);
		syncExposedSets(table);

		// 杠分即时结算
		settleGangScore(table, seat, MjExposedSet.Type.AN_GANG);

		// 暗杠后补牌
		ctx.setGangShangKaiHua(true);
		int drawnTile = drawTile(table);

		// 回放记录(暗杠+补牌)
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay != null) {
			replay.recordAnGang(seat, gangTileId, drawnTile >= 0 ? drawnTile : -1);
		}

		if (drawnTile >= 0) {
			if (checkZiMo(table, drawnTile)) {
				return true;
			}
			ctx.setGangShangKaiHua(false);
			table.upNextState(TableState.MJ_DISCARD);
		} else {
			finishGame(table, "暗杠后牌墙已空");
		}

		logger.info("麻将暗杠, table: {}, seat: {}, tile: {}", table.getTableId(), seat, gangTileId);
		return true;
	}

	/**
	 * 玩家主动补杠(出牌阶段选择)
	 * @param tileId 要补杠的牌ID
	 * @return true=执行了补杠(或被抢杠胡)
	 */
	public static boolean applyBuGang(MjTable table, int tileId) {
		int seat = table.getOp().getCurrOpSeat();
		TableUser user = table.getSeatUser(seat);
		if (user == null) return false;

		MjTableContext ctx = table.getMjContext();
		MjWinChecker winChecker = createWinChecker(table);

		if (!winChecker.canBuGang(user.getCards(), ctx.getExposedSets(seat), tileId)) {
			return false;
		}

		// 检查抢杠胡
		int seatNum = table.getTableModel().getSeatNum();
		for (int i = 1; i < seatNum; i++) {
			int checkSeat = (seat + i) % seatNum;
			TableUser other = table.getSeatUser(checkSeat);
			if (other == null) continue;

			List<Card> otherHand = other.getCards();
			List<Card> testHand = new ArrayList<>(otherHand);
			testHand.add(new Card(tileId));
			if (winChecker.canWin(testHand, ctx.getExposedSets(checkSeat), tileId)) {
				ctx.setQiangGangHu(true);
				user.removeCardsByProtoIds(Collections.singletonList(tileId));
				// 回放记录: 补杠被抢
				ReplayRecorder replay = table.getReplayRecorder();
				if (replay != null) {
					replay.recordBuGangRobbed(seat, tileId, checkSeat);
				}
				clearClaimState(table);
				processHu(table, checkSeat, tileId, seat, true);
				return true;
			}
		}

		// 没人抢杠, 执行补杠
		user.removeCardsByProtoIds(Collections.singletonList(tileId));

		// 更新副露: 找到碰, 改为补杠
		List<MjExposedSet> sets = ctx.getExposedSets(seat);
		for (int i = 0; i < sets.size(); i++) {
			MjExposedSet set = sets.get(i);
			if (set.getType() == MjExposedSet.Type.PENG && set.getTileIds().get(0) == tileId) {
				sets.set(i, new MjExposedSet(MjExposedSet.Type.BU_GANG,
						Arrays.asList(tileId, tileId, tileId, tileId), set.getFromSeat()));
				break;
			}
		}

		// 广播 + 副露同步
		broadcastMjAction(table, seat, tileId, GameProto.MjAction.MJ_GANG);
		syncExposedSets(table);

		// 杠分即时结算
		settleGangScore(table, seat, MjExposedSet.Type.BU_GANG);

		// 回放记录
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay != null) {
			int drawnForReplay = table.getMjTilePool().remaining() > 0 ? 0 : -1; // 补牌在后面
			replay.recordBuGang(seat, tileId, 0); // drawnTile在drawTile中记录
		}

		// 补杠后补牌
		ctx.setGangShangKaiHua(true);
		int drawnTile = drawTile(table);
		if (drawnTile >= 0) {
			if (checkZiMo(table, drawnTile)) {
				return true;
			}
			ctx.setGangShangKaiHua(false);
			table.upNextState(TableState.MJ_DISCARD);
		} else {
			finishGame(table, "补杠后牌墙已空");
		}

		logger.info("麻将补杠, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
		return true;
	}

	// ======================== 杠分即时结算 ========================

	/**
	 * 杠分即时结算(每次杠立即结算)
	 * 明杠: 每家出1分给杠者
	 * 暗杠: 每家出2分给杠者
	 * 补杠: 每家出1分给杠者
	 */
	private static void settleGangScore(MjTable table, int seat, MjExposedSet.Type gangType) {
		int gangScore = table.getTableModel().getGangScore();
		if (gangScore <= 0) gangScore = 1;

		int multiplier;
		switch (gangType) {
			case AN_GANG:
				multiplier = 2;
				break;
			case MING_GANG:
			case BU_GANG:
			default:
				multiplier = 1;
				break;
		}

		int scorePerPlayer = gangScore * multiplier;
		int seatNum = table.getTableModel().getSeatNum();
		int totalWin = 0;

		int[] scores = new int[seatNum];
		for (int i = 0; i < seatNum; i++) {
			if (i == seat) {
				scores[i] = scorePerPlayer * (seatNum - 1);
			} else {
				scores[i] = -scorePerPlayer;
			}
		}

		// 广播杠分结算(复用NotResult, winner=杠者, settle_factor=单人出分)
		GameProto.NotResult.Builder resultBuilder = GameProto.NotResult.newBuilder()
				.setWinner(seat)
				.setSettleFactor(scorePerPlayer);
		for (int i = 0; i < seatNum; i++) {
			TableUser u = table.getSeatUser(i);
			if (u != null) {
				resultBuilder.addRPlayers(GameProto.RPlayer.newBuilder()
						.setRoleId(u.getUserId()).build());
			}
		}
		table.sendTableMessage(resultBuilder.build(), GMsg.NOT_RESULT);

		logger.info("麻将杠分结算, table: {}, seat: {}, type: {}, score: {}",
				table.getTableId(), seat, gangType, scorePerPlayer);
	}

	// ======================== 流程控制 ========================

	/**
	 * 移动到下一个玩家
	 */
	public static void nextPlayer(MjTable table) {
		int currSeat = table.getOp().getCurrOpSeat();
		int nextSeat = table.nextSeat(currSeat);
		table.getOp().setCurrOpSeat(nextSeat);
		table.getMjContext().resetTurn();
		table.getMjContext().setGangShangKaiHua(false);
	}

	/**
	 * 结束本局(流局)
	 */
	public static void finishGame(MjTable table, String reason) {
		logger.info("麻将局结束, table: {}, round: {}, reason: {}", table.getTableId(), table.getCurrentRound(), reason);

		int seatNum = table.getTableModel().getSeatNum();
		int[] scores = new int[seatNum]; // 流局所有人0分

		// 回放记录
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay != null) {
			replay.recordDrawGame();
			writeFinalState(table, replay);
			replay.writeSettlement(-1, 0, "liuJu", scores);
			replay.save();
		}

		// 记录到整场结果
		table.getGameResult().addRound(table.getCurrentRound(), -1, 0, scores, "liuJu");

		// 发送单局结算
		sendRoundResult(table, -1, 0, scores, "liuJu");

		// 进入TABLE_OVER(等待玩家确认或自动下一局)
		table.upNextState(TableState.TABLE_OVER);
	}

	/**
	 * 胡牌结算
	 */
	private static void finishGameWithWin(MjTable table, MjWinResult winResult) {
		int seatNum = table.getTableModel().getSeatNum();
		MjScoring scoring = createScoring(table);
		MjTableContext ctx = table.getMjContext();
		TableModel model = table.getTableModel();

		int fan = scoring.calcFan(winResult, ctx, model);
		int[] scores = scoring.settle(winResult, fan, ctx, model, seatNum);

		// 确定胡牌方式
		String winType;
		if (winResult.isZiMo()) winType = "ziMo";
		else if (winResult.isGangShangKaiHua()) winType = "gangShangHua";
		else if (winResult.isQiangGangHu()) winType = "qiangGangHu";
		else if (winResult.isHaiDi()) winType = "haiDi";
		else winType = "dianPao";

		// 回放记录: 最终状态+结算
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay != null) {
			writeFinalState(table, replay);
			replay.writeSettlement(winResult.getWinnerId(), fan, winType, scores);
			replay.save();
		}

		// 记录到整场结果
		table.getGameResult().addRound(table.getCurrentRound(), winResult.getWinnerId(), fan, scores, winType);

		// 发送单局结算
		sendRoundResult(table, winResult.getWinnerId(), fan, scores, winType, winResult.getWinTile());

		logger.info("麻将胡牌, table: {}, round: {}, winner: {}, fan: {}, type: {}, scores: {}",
				table.getTableId(), table.getCurrentRound(), winResult.getWinnerId(), fan, winType,
				Arrays.toString(scores));

		// 进入TABLE_OVER(等待玩家确认或自动下一局)
		table.upNextState(TableState.TABLE_OVER);
	}

	/**
	 * 写入回放最终状态
	 */
	private static void writeFinalState(MjTable table, ReplayRecorder replay) {
		MjTableContext ctx = table.getMjContext();
		int seatNum = table.getTableModel().getSeatNum();
		Map<Integer, List<Integer>> finalHands = new HashMap<>();
		Map<Integer, List<MjExposedSet>> exposedMap = new HashMap<>();
		for (int i = 0; i < seatNum; i++) {
			TableUser u = table.getSeatUser(i);
			if (u != null) {
				finalHands.put(i, u.getCards().stream()
						.mapToInt(Card::getId).boxed().collect(Collectors.toList()));
			}
			exposedMap.put(i, ctx.getExposedSets(i));
		}
		replay.writeFinalState(finalHands, exposedMap, table.getMjTilePool().remaining());
	}

	/**
	 * 发送单局结算通知(带手牌)
	 */
	private static void sendRoundResult(MjTable table, int winnerSeat, int fan, int[] scores, String winType) {
		sendRoundResult(table, winnerSeat, fan, scores, winType, 0);
	}

	private static void sendRoundResult(MjTable table, int winnerSeat, int fan, int[] scores, String winType, int winTile) {
		int seatNum = table.getTableModel().getSeatNum();
		MjTableContext ctx = table.getMjContext();

		GameProto.NotRoundResult.Builder builder = GameProto.NotRoundResult.newBuilder()
				.setRound(table.getCurrentRound())
				.setWinnerSeat(winnerSeat)
				.setFan(fan)
				.setWinTile(winTile)
				.setWinType(ByteString.copyFromUtf8(winType));

		// 每家得分
		for (int i = 0; i < seatNum; i++) {
			builder.addSeatScores(GameProto.SeatScore.newBuilder()
					.setSeat(i).setScore(scores[i]).build());
		}

		// 每家副露统计
		for (int i = 0; i < seatNum; i++) {
			GameProto.SeatExposed.Builder seatExposed = GameProto.SeatExposed.newBuilder()
					.setSeat(i);
			for (MjExposedSet set : ctx.getExposedSets(i)) {
				String type;
				switch (set.getType()) {
					case PENG: type = "peng"; break;
					case MING_GANG: type = "mingGang"; break;
					case AN_GANG: type = "anGang"; break;
					case BU_GANG: type = "buGang"; break;
					case CHI: type = "chi"; break;
					default: type = "unknown"; break;
				}
				GameProto.ExposedInfo.Builder info = GameProto.ExposedInfo.newBuilder()
						.setType(ByteString.copyFromUtf8(type));
				for (int tileId : set.getTileIds()) {
					info.addTileIds(tileId);
				}
				seatExposed.addExposed(info.build());
			}
			builder.addSeatExposed(seatExposed.build());
		}

		// 每家手牌(结算展示用)
		for (int i = 0; i < seatNum; i++) {
			TableUser u = table.getSeatUser(i);
			GameProto.HandInfo.Builder handBuilder = GameProto.HandInfo.newBuilder()
					.setSeat(i);
			if (u != null) {
				for (Card c : u.getCards()) {
					handBuilder.addHandTiles(c.getId());
				}
			}
			for (MjExposedSet set : ctx.getExposedSets(i)) {
				String type;
				switch (set.getType()) {
					case PENG: type = "peng"; break;
					case MING_GANG: type = "mingGang"; break;
					case AN_GANG: type = "anGang"; break;
					case BU_GANG: type = "buGang"; break;
					case CHI: type = "chi"; break;
					default: type = "unknown"; break;
				}
				GameProto.ExposedInfo.Builder info = GameProto.ExposedInfo.newBuilder()
						.setType(ByteString.copyFromUtf8(type));
				for (int tileId : set.getTileIds()) {
					info.addTileIds(tileId);
				}
				handBuilder.addExposed(info.build());
			}
			builder.addHands(handBuilder.build());
		}

		table.sendTableMessage(builder.build(), GMsg.NOT_ROUND_RESULT);
	}

	/**
	 * 发送总结算通知
	 */
	public static void sendGameResult(MjTable table) {
		GameResult gameResult = table.getGameResult();
		int seatNum = table.getTableModel().getSeatNum();

		GameProto.NotGameResult.Builder builder = GameProto.NotGameResult.newBuilder()
				.setTotalRounds(gameResult.getTotalRounds())
				.setCompletedRounds(gameResult.getCompletedRounds());

		// 每家总分
		for (int i = 0; i < seatNum; i++) {
			builder.addTotalScores(GameProto.SeatScore.newBuilder()
					.setSeat(i).setScore(gameResult.getTotalScore(i)).build());
		}

		// 每局摘要
		for (GameResult.RoundEntry entry : gameResult.getRoundEntries()) {
			GameProto.RoundSummary.Builder summary = GameProto.RoundSummary.newBuilder()
					.setRound(entry.getRound())
					.setWinnerSeat(entry.getWinnerSeat())
					.setFan(entry.getFan())
					.setWinType(ByteString.copyFromUtf8(entry.getWinType()));
			for (int i = 0; i < seatNum; i++) {
				summary.addSeatScores(GameProto.SeatScore.newBuilder()
						.setSeat(i).setScore(entry.getScores()[i]).build());
			}
			builder.addRounds(summary.build());
		}

		table.sendTableMessage(builder.build(), GMsg.NOT_GAME_RESULT);
	}
	}

	// ======================== 副露区同步 ========================

	/**
	 * 广播所有玩家的副露区给客户端
	 */
	private static void syncExposedSets(MjTable table) {
		MjTableContext ctx = table.getMjContext();
		int seatNum = table.getTableModel().getSeatNum();

		GameProto.NotMjState.Builder notBuilder = GameProto.NotMjState.newBuilder()
				.setOpSeat(-1)
				.setAction(GameProto.MjAction.MJ_PASS)
				.setWallLeft(table.getMjTilePool().remaining());

		// 用choice字段携带副露信息: 每个座位的副露作为一个OpInfo
		for (int i = 0; i < seatNum; i++) {
			List<MjExposedSet> sets = ctx.getExposedSets(i);
			for (MjExposedSet set : sets) {
				GameProto.OpInfo.Builder opBuilder = GameProto.OpInfo.newBuilder();
				switch (set.getType()) {
					case PENG:
						opBuilder.setChoice(ConstProto.Operation.MJ_PENG);
						break;
					case MING_GANG:
					case AN_GANG:
					case BU_GANG:
						opBuilder.setChoice(ConstProto.Operation.MJ_GANG);
						break;
					case CHI:
						opBuilder.setChoice(ConstProto.Operation.MJ_CHI);
						break;
				}
				GameProto.CardInfo.Builder cardInfo = GameProto.CardInfo.newBuilder();
				for (int tileId : set.getTileIds()) {
					cardInfo.addCards(GameProto.Card.newBuilder().setValue(tileId).build());
				}
				opBuilder.addOpCards(cardInfo.build());
				notBuilder.addChoice(opBuilder.build());
			}
		}

		table.sendTableMessage(notBuilder.build(), GMsg.MJ_TILE_NOT);
	}

	// ======================== 赖子翻牌 ========================

	/**
	 * 翻牌确定赖子(荆门麻将)
	 */
	public static void flipLaiZi(MjTable table) {
		MjTilePool tilePool = table.getMjTilePool();
		if (tilePool == null || tilePool.remaining() <= 0) {
			return;
		}

		int flipTile = tilePool.drawTile();
		MjTableContext ctx = table.getMjContext();
		ctx.setLaiZiFlipTile(flipTile);

		int laiZi = nextTile(flipTile);
		ctx.setLaiZiTileId(laiZi);

		GameProto.NotMjState not = GameProto.NotMjState.newBuilder()
				.setOpSeat(-1)
				.setTileId(flipTile)
				.setAction(GameProto.MjAction.MJ_DRAW)
				.setWallLeft(tilePool.remaining())
				.build();
		table.sendTableMessage(not, GMsg.MJ_TILE_NOT);

		logger.info("麻将翻牌确定赖子, table: {}, flipTile: {}, laiZi: {}",
				table.getTableId(), flipTile, laiZi);
	}

	/**
	 * 获取下一张牌(赖子计算)
	 */
	private static int nextTile(int tileId) {
		int suit = MjConst.suitOf(tileId);
		int value = MjConst.valueOf(tileId);

		if (suit <= MjConst.SUIT_TONG) {
			int nextValue = value >= 9 ? 1 : value + 1;
			return MjConst.encode(suit, nextValue);
		} else if (suit == MjConst.SUIT_FENG) {
			int nextValue = value >= 4 ? 1 : value + 1;
			return MjConst.encode(suit, nextValue);
		} else {
			int nextValue = value >= 3 ? 1 : value + 1;
			return MjConst.encode(suit, nextValue);
		}
	}

	// ======================== 工具方法 ========================

	/**
	 * 广播麻将操作
	 */
	private static void broadcastMjAction(MjTable table, int seat, int tileId, GameProto.MjAction action) {
		GameProto.NotMjState not = GameProto.NotMjState.newBuilder()
				.setOpSeat(seat)
				.setTileId(tileId)
				.setAction(action)
				.setWallLeft(table.getMjTilePool().remaining())
				.build();
		table.sendTableMessage(not, GMsg.MJ_TILE_NOT);
	}

	/**
	 * 根据桌子配置创建WinChecker
	 */
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

	/**
	 * 根据桌子配置创建Scoring
	 */
	public static MjScoring createScoring(MjTable table) {
		int subType = table.getTableModel().getGameSubType();

		switch (subType) {
			case 1: // 荆门
				return new JmMjScoring();
			case 2: // 卡五星
				return new KwMjScoring((KwWinChecker) createWinChecker(table));
			default:
				return new JmMjScoring();
		}
	}
}
