package game.manager.table.mj;

import game.manager.table.MjTable;
import game.manager.table.TableUser;
import game.manager.table.cards.Card;
import game.manager.table.replay.MjReplayRecorder;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import proto.ConstProto;
import proto.GameProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 麻将claim服务
 * 处理碰/杠/胡/吃检测、claim响应、超时处理
 */
public class MjClaimService {

	private static final Logger logger = LoggerFactory.getLogger(MjClaimService.class);

	private MjClaimService() {}

	// ======================== Claim检测 ========================

	/** 为指定座位构建claim信息（供AI决策和主流程共用） */
	public static MjClaimInfo buildClaimInfo(MjTable table, int seat) {
		MjTableContext ctx = table.getMjContext();
		int tileId = ctx.getClaimTileId();
		if (tileId == 0) return null;
		TableUser user = table.getSeatUser(seat);
		if (user == null) return null;
		return detectSeatClaim(table, seat, user.getCards(), ctx.getExposedSets(seat), tileId, ctx.getClaimFromSeat());
	}

	/** 检测单个座位对指定牌的claim能力（胡/杠/碰/吃），返回null表示无claim */
	static MjClaimInfo detectSeatClaim(MjTable table, int seat, List<Card> handTiles,
			List<MjExposedSet> exposedSets, int tileId, int fromSeat) {
		boolean allowHu = table.getTableModel().getAllowHu() != 0;
		boolean allowPeng = table.getTableModel().getAllowPeng() != 0;
		boolean allowGangMing = table.getTableModel().getAllowGangMing() != 0;
		boolean allowChi = table.getTableModel().getAllowChi() == 1;
		MjWinChecker winChecker = MjPlayService.createWinChecker(table);

		boolean canHu = false, canGang = false, canPeng = false, canChi = false;
		int gangTileId = 0;
		List<int[]> chiCombos = new ArrayList<>();

		if (allowHu) {
			List<Card> testHand = new ArrayList<>(handTiles);
			testHand.add(new Card(tileId));
			if (winChecker.canWin(testHand, exposedSets, tileId)) canHu = true;
		}
		if (allowPeng && winChecker.canPeng(handTiles, tileId)) canPeng = true;
		if (allowGangMing && winChecker.canMingGang(handTiles, tileId)) {
			canGang = true;
			gangTileId = tileId;
		}
		if (allowChi) {
			int seatNum = table.getTableModel().getSeatNum();
			int prevSeat = (fromSeat + seatNum - 1) % seatNum;
			if (seat == prevSeat) {
				chiCombos = winChecker.getChiCombos(handTiles, tileId);
				canChi = !chiCombos.isEmpty();
			}
		}

		if (!canHu && !canGang && !canPeng && !canChi) return null;
		return new MjClaimInfo(seat, canHu, canGang, canPeng, canChi, tileId, gangTileId, chiCombos);
	}

	/** 出牌后检测所有座位的claim，有则进入MJ_CLAIM状态 */
	public static boolean checkClaim(MjTable table) {
		MjTableContext ctx = table.getMjContext();
		int tileId = ctx.getLastDiscardTile();
		int fromSeat = ctx.getLastDiscardSeat();
		if (tileId == 0) return false;

		int seatNum = table.getTableModel().getSeatNum();
		List<MjClaimInfo> claims = new ArrayList<>();
		List<Integer> waitingSeats = new ArrayList<>();

		for (int i = 1; i < seatNum; i++) {
			int checkSeat = (fromSeat + i) % seatNum;
			TableUser user = table.getSeatUser(checkSeat);
			if (user == null) continue;
			MjClaimInfo claim = detectSeatClaim(table, checkSeat, user.getCards(),
					ctx.getExposedSets(checkSeat), tileId, fromSeat);
			if (claim != null) {
				claims.add(claim);
				waitingSeats.add(checkSeat);
			}
		}

		if (claims.isEmpty()) return false;

		ctx.setClaimInfo(tileId, fromSeat, waitingSeats);
		for (MjClaimInfo claim : claims) sendClaimOptions(table, claim);
		table.upNextState(TableState.MJ_CLAIM);

		logger.info("麻将claim检测, table: {}, tile: {}, claimSeats: {}", table.getTableId(), tileId, waitingSeats);
		return true;
	}

	/** 给单个座位发送claim可选操作 */
	private static void sendClaimOptions(MjTable table, MjClaimInfo claim) {
		GameProto.NotMjState.Builder notBuilder = GameProto.NotMjState.newBuilder()
				.setOpSeat(claim.getSeat())
				.setTileId(table.getMjContext().getClaimTileId())
				.setAction(ConstProto.Operation.DISCARD)
				.setWait(TableState.MJ_CLAIM.getOverTime())
				.setWallLeft(table.getMjTilePool().remaining());

		if (claim.isCanHu()) {
			notBuilder.addChoice(GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.MJ_HU).build());
		}
		if (claim.isCanGang()) {
			notBuilder.addChoice(GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.MJ_GANG).build());
		}
		if (claim.isCanPeng()) {
			notBuilder.addChoice(GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.MJ_PENG).build());
		}
		if (claim.isCanChi()) {
			for (int[] combo : claim.getChiCombos()) {
				GameProto.OpInfo.Builder chiBuilder = GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.MJ_CHI);
				GameProto.CardInfo.Builder cardInfoBuilder = GameProto.CardInfo.newBuilder();
				for (int chiTile : combo) {
					cardInfoBuilder.addCards(GameProto.Card.newBuilder().setValue(chiTile).build());
				}
				chiBuilder.addOpCards(cardInfoBuilder.build());
				notBuilder.addChoice(chiBuilder.build());
			}
		}
		notBuilder.addChoice(GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.MJ_PASS).build());

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

	/** 处理玩家的claim响应(碰/杠/胡/吃/过) */
	public static boolean applyClaim(MjTable table, int userId, GameProto.OpInfo op) {
		MjTableContext ctx = table.getMjContext();
		int seat = -1;
		for (Map.Entry<Integer, TableUser> entry : table.getSeatUsers().entrySet()) {
			if (entry.getValue().getUserId() == userId) {
				seat = entry.getKey();
				break;
			}
		}
		if (seat < 0 || !ctx.getPendingClaimSeats().contains(seat)) return false;

		ConstProto.Operation choice = op.getChoice();
		int tileId = ctx.getClaimTileId();
		int fromSeat = ctx.getClaimFromSeat();

		switch (choice) {
			case MJ_HU: return processHu(table, seat, tileId, fromSeat, false);
			case MJ_GANG: return processClaimGang(table, seat, tileId, fromSeat);
			case MJ_PENG: return processPeng(table, seat, tileId, fromSeat);
			case MJ_CHI: return processChi(table, seat, tileId, fromSeat, op);
			case MJ_PASS: return processPass(table, seat);
			default:
				logger.warn("无效的claim操作, table: {}, userId: {}, choice: {}", table.getTableId(), userId, choice);
				return false;
		}
	}

	/** 清理claim状态并取消所有待响应的座位（包内可见，供MjDrawService调用） */
	public static void clearClaimState(MjTable table) {
		MjTableContext ctx = table.getMjContext();
		ctx.getPendingClaimSeats().clear();
		ctx.setClaimInfo(0, 0, Collections.emptyList());
	}

	/** claim超时: 所有待响应座位自动pass */
	public static void timeoutClaim(MjTable table) {
		MjTableContext ctx = table.getMjContext();
		List<Integer> pending = new ArrayList<>(ctx.getPendingClaimSeats());
		MjReplayRecorder replay = (MjReplayRecorder) table.getReplayRecorder();
		for (int seat : pending) {
			if (replay != null) replay.recordAutoPass(seat);
			ctx.removeClaimSeat(seat);
		}
		clearClaimState(table);
		MjPlayService.nextPlayer(table);
		table.upNextState(TableState.MJ_PLAY);
	}

	// ======================== 具体操作处理 ========================

	/** 处理胡牌（包内可见，供MjGangService调用） */
	static boolean processHu(MjTable table, int seat, int tileId, int fromSeat, boolean qiangGang) {
		TableUser user = table.getSeatUser(seat);
		if (user == null) return false;

		MjTableContext ctx = table.getMjContext();
		if (!qiangGang) user.addCards(new Card(tileId));

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

		clearClaimState(table);
		MjReplayRecorder replay = (MjReplayRecorder) table.getReplayRecorder();
		if (replay != null) replay.recordHu(seat, tileId, false);

		MjSettleService.finishGameWithWin(table, winResult);
		return true;
	}

	/** 处理碰 */
	private static boolean processPeng(MjTable table, int seat, int tileId, int fromSeat) {
		TableUser user = table.getSeatUser(seat);
		if (user == null) return false;

		int removed = removeCardsById(user, tileId, 2);
		if (removed < 2) {
			logger.error("碰牌时手牌不足, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
			return false;
		}

		MjTableContext ctx = table.getMjContext();
		ctx.addExposedSet(seat, new MjExposedSet(MjExposedSet.Type.PENG,
				Arrays.asList(tileId, tileId, tileId), fromSeat));
		clearClaimState(table);
		table.getOp().setCurrOpSeat(seat);
		ctx.resetTurn();

		MjSettleService.broadcastMjAction(table, seat, tileId, ConstProto.Operation.MJ_PENG);
		MjSettleService.syncExposedSets(table);

		MjReplayRecorder replay = (MjReplayRecorder) table.getReplayRecorder();
		if (replay != null) replay.recordPeng(seat, fromSeat, tileId);

		table.upNextState(TableState.MJ_DISCARD);
		logger.info("麻将碰, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
		return true;
	}

	/** 处理claim明杠 */
	private static boolean processClaimGang(MjTable table, int seat, int tileId, int fromSeat) {
		TableUser user = table.getSeatUser(seat);
		if (user == null) return false;

		int removed = removeCardsById(user, tileId, 3);
		if (removed < 3) {
			logger.error("杠牌时手牌不足, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
			return false;
		}

		MjTableContext ctx = table.getMjContext();
		ctx.addExposedSet(seat, new MjExposedSet(MjExposedSet.Type.MING_GANG,
				Arrays.asList(tileId, tileId, tileId, tileId), fromSeat));
		clearClaimState(table);
		table.getOp().setCurrOpSeat(seat);
		ctx.resetTurn();
		ctx.setGangShangKaiHua(true);

		MjSettleService.broadcastMjAction(table, seat, tileId, ConstProto.Operation.MJ_GANG);
		MjSettleService.syncExposedSets(table);

		MjReplayRecorder replay = (MjReplayRecorder) table.getReplayRecorder();
		if (replay != null) replay.recordMingGang(seat, fromSeat, tileId);

		MjGangService.settleGangScore(table, seat, MjExposedSet.Type.MING_GANG);

		int drawnTile = MjDrawService.drawTile(table);
		if (drawnTile >= 0) {
			List<Card> handTiles = user.getCards();
			MjWinChecker winChecker = MjPlayService.createWinChecker(table);
			if (winChecker.canWin(handTiles, ctx.getExposedSets(seat), drawnTile)) {
				MjDrawService.processZiMo(table, seat, drawnTile);
				return true;
			}
			ctx.setGangShangKaiHua(false);
			table.upNextState(TableState.MJ_DISCARD);
		} else {
			MjSettleService.finishGame(table, "杠后牌墙已空");
		}

		logger.info("麻将明杠, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
		return true;
	}

	/** 处理claim吃 */
	private static boolean processChi(MjTable table, int seat, int tileId, int fromSeat, GameProto.OpInfo op) {
		TableUser user = table.getSeatUser(seat);
		if (user == null) return false;
		if (op.getOpCardsCount() == 0) return false;

		List<Integer> chiTileIds = new ArrayList<>();
		for (GameProto.Card c : op.getOpCards(0).getCardsList()) {
			chiTileIds.add(c.getValue());
		}
		chiTileIds.add(tileId);

		for (int chiTile : chiTileIds) {
			if (chiTile == tileId) continue;
			boolean removed = user.removeCardsByProtoIds(Collections.singletonList(chiTile));
			if (!removed) {
				logger.error("吃牌时手牌不足, table: {}, seat: {}, tile: {}", table.getTableId(), seat, chiTile);
				return false;
			}
		}

		MjTableContext ctx = table.getMjContext();
		ctx.addExposedSet(seat, new MjExposedSet(MjExposedSet.Type.CHI, chiTileIds, fromSeat));
		clearClaimState(table);
		table.getOp().setCurrOpSeat(seat);
		ctx.resetTurn();

		MjSettleService.broadcastMjAction(table, seat, tileId, ConstProto.Operation.MJ_CHI);
		MjSettleService.syncExposedSets(table);

		MjReplayRecorder replay = (MjReplayRecorder) table.getReplayRecorder();
		if (replay != null) {
			replay.recordChi(seat, fromSeat, chiTileIds, user.getCards().stream()
					.mapToInt(Card::getId).boxed().collect(Collectors.toList()));
		}

		table.upNextState(TableState.MJ_DISCARD);
		logger.info("麻将吃, table: {}, seat: {}, tiles: {}", table.getTableId(), seat, chiTileIds);
		return true;
	}

	/** 处理pass */
	private static boolean processPass(MjTable table, int seat) {
		MjTableContext ctx = table.getMjContext();
		ctx.removeClaimSeat(seat);
		if (!ctx.hasPendingClaims()) {
			MjPlayService.nextPlayer(table);
			table.upNextState(TableState.MJ_PLAY);
		}
		return true;
	}

	// ======================== 工具方法 ========================

	/** 从手牌中按ID移除指定张数，返回实际移除数 */
	private static int removeCardsById(TableUser user, int tileId, int count) {
		int removed = 0;
		Iterator<Card> it = user.getCards().iterator();
		while (it.hasNext() && removed < count) {
			if (it.next().getId() == tileId) {
				it.remove();
				removed++;
			}
		}
		return removed;
	}
}
