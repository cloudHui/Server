package game.manager.table.ddz;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import game.manager.table.DdzTable;
import game.manager.table.GameResult;
import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.cards.Card;
import game.manager.table.ddz.ai.DdzSimpleAi;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import proto.ConstProto;
import proto.GameProto;

/**
 * 出牌阶段：过牌、出牌、比大小、胜负结算。
 * 
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 */
public final class DdzPlayService {

	private static final Logger logger = LoggerFactory.getLogger(DdzPlayService.class);

	private DdzPlayService() {
	}

	/**
	 * 自动出最小牌
	 * 
	 * @param table  桌子
	 * @param userId 用户ID
	 */
	public static void autoPlaySmallest(DdzTable table, int userId) {
		TableUser user = table.getUsers().get(userId);
		if (user == null || user.getCards().isEmpty()) {
			return;
		}
		List<Card> hand = user.getCards();
		Card smallest = Collections.min(hand);
		GameProto.OpInfo op = GameProto.OpInfo.newBuilder()
				.setChoice(ConstProto.Operation.PLAY)
				.addOpCards(GameProto.CardInfo.newBuilder()
						.addCards(GameProto.Card.newBuilder().setValue(smallest.getId()).build())
						.build())
				.build();
		apply(table, userId, op);
	}

	/**
	 * 托管/超时：走简易 AI（拆牌+合法压制+启发式），失败返回 false。
	 * 
	 * @param table  桌子
	 * @param userId 用户ID
	 * @return 是否成功
	 */
	public static boolean autoPlayAi(DdzTable table, int userId) {
		TableUser user = table.getUsers().get(userId);
		if (user == null) {
			return false;
		}
		GameProto.OpInfo op = DdzSimpleAi.decide(table, user);
		return apply(table, userId, op) == ConstProto.Result.SUCCESS_VALUE;
	}

	/**
	 * 应用操作
	 * 
	 * @param table  桌子
	 * @param userId 用户ID
	 * @param opInfo 操作信息
	 * @return 结果
	 */
	public static int apply(DdzTable table, int userId, GameProto.OpInfo opInfo) {
		if (table.getTableState() != TableState.IDLE_CARD) {
			return ConstProto.Result.OP_CURR_ERROR_VALUE;
		}
		TableUser user = table.getUsers().get(userId);
		if (user == null || user.getSeated() != table.getOp().getCurrOpSeat()) {
			return ConstProto.Result.OP_CURR_ERROR_VALUE;
		}
		ConstProto.Operation choice = opInfo.getChoice();
		if (choice == ConstProto.Operation.PASS) {
			return applyPass(table, userId);
		}
		if (choice == ConstProto.Operation.PLAY) {
			return applyPlay(table, user, opInfo);
		}
		return ConstProto.Result.OP_CURR_ERROR_VALUE;
	}

	/**
	 * 应用过牌
	 * 
	 * @param table  桌子
	 * @param userId 用户ID
	 * @return 结果
	 */
	private static int applyPass(DdzTable table, int userId) {
		DdzTableContext ctx = table.getDdz();
		if (ctx.getLastHand() == null) {
			return ConstProto.Result.OP_CURR_ERROR_VALUE;
		}
		broadcastAck(table, userId, GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.PASS).build());
		ctx.addPass();
		if (ctx.getConsecutivePasses() >= 2) {
			int leader = ctx.getLastPlaySeat();
			ctx.resetCurrentTrickCards();
			table.getOp().setCurrOpSeat(leader);
		} else {
			table.getOp().moveToNextOp();
		}
		table.getBanner().setRobBroadcastDone(false);
		table.upNextStateWithTime(TableState.CARD, System.currentTimeMillis());
		return ConstProto.Result.SUCCESS_VALUE;
	}

	/**
	 * 应用出牌
	 * 
	 * @param table  桌子
	 * @param user   用户
	 * @param opInfo 操作信息
	 * @return 结果
	 */
	private static int applyPlay(DdzTable table, TableUser user, GameProto.OpInfo opInfo) {
		DdzTableContext ctx = table.getDdz();
		List<Integer> ids = collectPlayedCardIds(opInfo);
		if (ids.isEmpty()) {
			return ConstProto.Result.OP_CARD_NOT_MATCH_VALUE;
		}
		List<Card> selected = pullFromHand(user, ids);
		if (selected == null) {
			return ConstProto.Result.OP_CARD_NOT_MATCH_VALUE;
		}
		java.util.Optional<DdzHand> parsed = DdzRules.analyze(selected);
		if (!parsed.isPresent()) {
			return ConstProto.Result.OP_CARD_NOT_MATCH_VALUE;
		}
		DdzHand hand = parsed.get();
		if (ctx.getLastHand() == null) {
			if (!user.removeCardsByProtoIds(ids)) {
				return ConstProto.Result.OP_CARD_NOT_MATCH_VALUE;
			}
			afterSuccessfulPlay(table, user, hand);
			return ConstProto.Result.SUCCESS_VALUE;
		}
		if (!DdzRules.beats(hand, ctx.getLastHand())) {
			return ConstProto.Result.OP_CARD_NOT_MATCH_VALUE;
		}
		if (!user.removeCardsByProtoIds(ids)) {
			return ConstProto.Result.OP_CARD_NOT_MATCH_VALUE;
		}
		afterSuccessfulPlay(table, user, hand);
		return ConstProto.Result.SUCCESS_VALUE;
	}

	/**
	 * 成功出牌后处理
	 * 
	 * @param table 桌子
	 * @param user  用户
	 * @param hand  手牌
	 */
	private static void afterSuccessfulPlay(DdzTable table, TableUser user, DdzHand hand) {
		DdzTableContext ctx = table.getDdz();
		int landlordSeat = ctx.getLandlordSeat();
		if (user.getSeated() == landlordSeat) {
			ctx.incrementLandlordPlayCount();
		} else {
			ctx.setFarmerEverPlayed(true);
		}
		ctx.recordPlayedCards(hand.getCards());
		broadcastAck(table, user.getUserId(), GameProto.OpInfo.newBuilder()
				.setChoice(ConstProto.Operation.PLAY)
				.addOpCards(hand.toCardInfo())
				.build());
		ctx.setLastHand(hand);
		ctx.setLastPlayed(hand.toCardInfo());
		ctx.setConsecutivePasses(0);
		ctx.setLastPlaySeat(user.getSeated());
		if (user.getCards().isEmpty()) {
			finishGame(table, user);
			return;
		}
		table.getOp().moveToNextOp();
		table.getBanner().setRobBroadcastDone(false);
		table.upNextStateWithTime(TableState.CARD, System.currentTimeMillis());
	}

	/**
	 * 结束游戏
	 * 
	 * @param table  桌子
	 * @param winner 赢家
	 */
	private static void finishGame(DdzTable table, TableUser winner) {
		DdzTableContext ctx = table.getDdz();
		int landlordSeat = ctx.getLandlordSeat();
		TableUser landlordUser = table.getSeatUser(landlordSeat);
		int landlordUserId = landlordUser != null ? landlordUser.getUserId() : 0;

		boolean landlordWin = winner.getSeated() == landlordSeat;
		int winTeam = landlordWin ? 0 : 1;

		boolean spring = landlordWin && !ctx.isFarmerEverPlayed();
		boolean antiSpring = !landlordWin && ctx.getLandlordPlayCount() <= 1;

		int settleFactor = ctx.getBaseScore() * ctx.getRobMultiplier();
		if (spring) {
			settleFactor *= 2;
		}
		if (antiSpring) {
			settleFactor *= 2;
		}

		// 计算每家得分(地主赢: 地主+2*settleFactor, 农民各-settleFactor)
		int seatNum = table.getTableModel().getSeatNum();
		int[] scores = new int[seatNum];
		for (int i = 0; i < seatNum; i++) {
			if (i == landlordSeat) {
				scores[i] = landlordWin ? settleFactor * (seatNum - 1) : -settleFactor * (seatNum - 1);
			} else {
				scores[i] = landlordWin ? -settleFactor : settleFactor;
			}
		}

		// 记录到整场结果
		String winType = spring ? "spring" : (antiSpring ? "antiSpring" : "normal");
		table.getGameResult().addRound(table.getCurrentRound(), winner.getSeated(), settleFactor, scores, winType);

		List<GameProto.RPlayer> rPlayers = new ArrayList<>();
		for (TableUser u : table.getSeatUsers().values()) {
			GameProto.RPlayer.Builder rp = GameProto.RPlayer.newBuilder().setRoleId(u.getUserId());
			for (Card c : u.getCards()) {
				rp.addCards(GameProto.Card.newBuilder().setValue(c.getId()).build());
			}
			rPlayers.add(rp.build());
		}

		try {
			byte[] payload = DdzResultEncoder.encodeNotResultExtended(
					winner.getUserId(),
					rPlayers,
					landlordUserId,
					winTeam,
					ctx.getBaseScore(),
					ctx.getRobMultiplier(),
					spring,
					antiSpring,
					settleFactor);
			table.sendTableMessageRaw(GMsg.NOT_RESULT, payload);
		} catch (IOException e) {
			logger.error("encode NotResult failed table:{}", table.getTableId(), e);
			GameProto.NotResult.Builder b = GameProto.NotResult.newBuilder().setWinner(winner.getUserId());
			for (GameProto.RPlayer rp : rPlayers) {
				b.addRPlayers(rp);
			}
			table.sendTableMessage(b.build(), GMsg.NOT_RESULT);
		}

		// 发送单局结算(多局模式)
		if (table.isMultiRound()) {
			GameProto.NotRoundResult.Builder roundResult = GameProto.NotRoundResult.newBuilder()
					.setRound(table.getCurrentRound())
					.setWinnerSeat(winner.getSeated())
					.setFan(settleFactor)
					.setWinType(ByteString.copyFromUtf8(winType));
			for (int i = 0; i < seatNum; i++) {
				roundResult.addSeatScores(GameProto.SeatScore.newBuilder()
						.setSeat(i).setScore(scores[i]).build());
			}
			table.sendTableMessage(roundResult.build(), GMsg.NOT_ROUND_RESULT);
		}

		table.upNextStateWithTime(TableState.TABLE_OVER, System.currentTimeMillis());
	}

	/**
	 * 收集出牌的卡片ID
	 * 
	 * @param opInfo 操作信息
	 * @return 卡片ID列表
	 */
	private static List<Integer> collectPlayedCardIds(GameProto.OpInfo opInfo) {
		List<Integer> ids = new ArrayList<>();
		for (GameProto.CardInfo ci : opInfo.getOpCardsList()) {
			for (GameProto.Card c : ci.getCardsList()) {
				ids.add(c.getValue());
			}
		}
		return ids;
	}

	/**
	 * 从手牌中提取卡片
	 * 
	 * @param user 用户
	 * @param ids  卡片ID列表
	 * @return 提取的卡片列表
	 */
	private static List<Card> pullFromHand(TableUser user, List<Integer> ids) {
		List<Card> handSnapshot = new ArrayList<>(user.getCards());
		List<Card> out = new ArrayList<>();
		for (int id : ids) {
			Card found = null;
			for (Card c : handSnapshot) {
				if (c.getId() == id) {
					found = c;
					break;
				}
			}
			if (found == null) {
				return null;
			}
			handSnapshot.remove(found);
			out.add(found);
		}
		return out;
	}

	/**
	 * 广播确认
	 * 
	 * @param table       桌子
	 * @param actorUserId 用户ID
	 * @param op          操作信息
	 */
	private static void broadcastAck(DdzTable table, int actorUserId, GameProto.OpInfo op) {
		GameProto.AckOp msg = GameProto.AckOp.newBuilder()
				.setOp(op)
				.setOpId(actorUserId)
				.setOpFrom(actorUserId)
				.build();
		table.sendTableMessage(msg, GMsg.ACK_OP);
	}

	/**
	 * 发送DDZ总结算通知(多局汇总)
	 */
	public static void sendGameResult(Table table) {
		GameResult gameResult = table.getGameResult();
		int seatNum = table.getTableModel().getSeatNum();

		GameProto.NotGameResult.Builder builder = GameProto.NotGameResult.newBuilder()
				.setTotalRounds(gameResult.getTotalRounds())
				.setCompletedRounds(gameResult.getCompletedRounds());

		for (int i = 0; i < seatNum; i++) {
			builder.addTotalScores(GameProto.SeatScore.newBuilder()
					.setSeat(i).setScore(gameResult.getTotalScore(i)).build());
		}

		for (GameResult.RoundEntry entry : gameResult.getRoundEntries()) {
			GameProto.RoundSummary.Builder summary = GameProto.RoundSummary.newBuilder()
					.setRound(entry.getRound())
					.setWinnerSeat(entry.getWinnerSeat())
					.setFan(entry.getScore())
					.setWinType(com.google.protobuf.ByteString.copyFromUtf8(entry.getWinType()));
			for (int i = 0; i < seatNum; i++) {
				summary.addSeatScores(GameProto.SeatScore.newBuilder()
						.setSeat(i).setScore(entry.getScores()[i]).build());
			}
			builder.addRounds(summary.build());
		}

		table.sendTableMessage(builder.build(), GMsg.NOT_GAME_RESULT);
	}
}
