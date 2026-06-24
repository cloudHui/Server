package game.manager.table.ddz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import game.manager.table.DdzTable;
import game.manager.table.TableUser;
import game.manager.table.cards.Card;
import game.manager.table.ddz.ai.DdzSimpleAi;
import game.manager.table.replay.DdzReplayRecorder;
import game.manager.table.replay.ReplayRecorder;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import proto.ConstProto;
import proto.GameProto;

/**
 * 斗地主出牌服务
 * 处理出牌、过牌、自动出牌、AI出牌
 */
public final class DdzPlayService {

	private static final Logger logger = LoggerFactory.getLogger(DdzPlayService.class);

	private DdzPlayService() {}

	/** 超时/托管自动出最小牌 */
	public static void autoPlaySmallest(DdzTable table, int userId) {
		logger.info("斗地主超时自动出最小牌, tableId: {}, userId: {}", table.getTableId(), userId);
		TableUser user = table.getUsers().get(userId);
		if (user == null || user.getCards().isEmpty()) return;

		List<Card> hand = user.getCards();
		Card smallest = Collections.min(hand);

		ReplayRecorder replay = table.getReplayRecorder();
		if (replay instanceof DdzReplayRecorder) {
			((DdzReplayRecorder) replay).recordAutoPlay(user.getSeated(), Collections.singletonList(smallest.getId()));
		}

		GameProto.OpInfo op = GameProto.OpInfo.newBuilder()
				.setChoice(ConstProto.Operation.PLAY)
				.addOpCards(GameProto.CardInfo.newBuilder()
						.addCards(GameProto.Card.newBuilder().setValue(smallest.getId()).build())
						.build())
				.build();
		apply(table, userId, op);
	}

	/** 托管/超时：走简易AI，失败返回false */
	public static boolean autoPlayAi(DdzTable table, int userId) {
		TableUser user = table.getUsers().get(userId);
		if (user == null) return false;
		GameProto.OpInfo op = DdzSimpleAi.decide(table, user);
		return apply(table, userId, op) == ConstProto.Result.SUCCESS_VALUE;
	}

	/** 应用操作（出牌/过牌） */
	public static int apply(DdzTable table, int userId, GameProto.OpInfo opInfo) {
		if (table.getTableState() != TableState.IDLE_CARD) return ConstProto.Result.OP_CURR_ERROR_VALUE;
		TableUser user = table.getUsers().get(userId);
		if (user == null || user.getSeated() != table.getOp().getCurrOpSeat()) return ConstProto.Result.OP_CURR_ERROR_VALUE;

		ConstProto.Operation choice = opInfo.getChoice();
		if (choice == ConstProto.Operation.PASS) return applyPass(table, userId);
		if (choice == ConstProto.Operation.PLAY) return applyPlay(table, user, opInfo);
		return ConstProto.Result.OP_CURR_ERROR_VALUE;
	}

	// ======================== 内部方法 ========================

	/** 应用过牌 */
	private static int applyPass(DdzTable table, int userId) {
		DdzTableContext ctx = table.getDdz();
		if (ctx.getLastHand() == null) return ConstProto.Result.OP_CURR_ERROR_VALUE;

		TableUser user = table.getUsers().get(userId);
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay instanceof DdzReplayRecorder && user != null) {
			((DdzReplayRecorder) replay).recordPass(user.getSeated());
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

	/** 应用出牌 */
	private static int applyPlay(DdzTable table, TableUser user, GameProto.OpInfo opInfo) {
		DdzTableContext ctx = table.getDdz();
		List<Integer> ids = collectPlayedCardIds(opInfo);
		if (ids.isEmpty()) return ConstProto.Result.OP_CARD_NOT_MATCH_VALUE;

		List<Card> selected = pullFromHand(user, ids);
		if (selected == null) return ConstProto.Result.OP_CARD_NOT_MATCH_VALUE;

		java.util.Optional<DdzHand> parsed = DdzRules.analyze(selected);
		if (!parsed.isPresent()) return ConstProto.Result.OP_CARD_NOT_MATCH_VALUE;

		DdzHand hand = parsed.get();
		if (ctx.getLastHand() == null) {
			if (!user.removeCardsByProtoIds(ids)) return ConstProto.Result.OP_CARD_NOT_MATCH_VALUE;
			afterSuccessfulPlay(table, user, hand);
			return ConstProto.Result.SUCCESS_VALUE;
		}
		if (!DdzRules.beats(hand, ctx.getLastHand())) return ConstProto.Result.OP_CARD_NOT_MATCH_VALUE;
		if (!user.removeCardsByProtoIds(ids)) return ConstProto.Result.OP_CARD_NOT_MATCH_VALUE;
		afterSuccessfulPlay(table, user, hand);
		return ConstProto.Result.SUCCESS_VALUE;
	}

	/** 成功出牌后处理 */
	private static void afterSuccessfulPlay(DdzTable table, TableUser user, DdzHand hand) {
		DdzTableContext ctx = table.getDdz();
		int landlordSeat = ctx.getLandlordSeat();
		if (user.getSeated() == landlordSeat) {
			ctx.incrementLandlordPlayCount();
		} else {
			ctx.setFarmerEverPlayed(true);
		}
		ctx.recordPlayedCards(hand.getCards());

		ReplayRecorder replay = table.getReplayRecorder();
		if (replay instanceof DdzReplayRecorder) {
			List<Integer> ids = new ArrayList<>();
			for (Card c : hand.getCards()) ids.add(c.getId());
			((DdzReplayRecorder) replay).recordPlay(user.getSeated(), ids);
		}

		broadcastAck(table, user.getUserId(), GameProto.OpInfo.newBuilder()
				.setChoice(ConstProto.Operation.PLAY)
				.addOpCards(hand.toCardInfo()).build());
		ctx.setLastHand(hand);
		ctx.setLastPlayed(hand.toCardInfo());
		ctx.setConsecutivePasses(0);
		ctx.setLastPlaySeat(user.getSeated());

		if (user.getCards().isEmpty()) {
			DdzSettleService.finishGame(table, user);
			return;
		}
		table.getOp().moveToNextOp();
		table.getBanner().setRobBroadcastDone(false);
		table.upNextStateWithTime(TableState.CARD, System.currentTimeMillis());
	}

	/** 收集出牌的卡片ID */
	private static List<Integer> collectPlayedCardIds(GameProto.OpInfo opInfo) {
		List<Integer> ids = new ArrayList<>();
		for (GameProto.CardInfo ci : opInfo.getOpCardsList()) {
			for (GameProto.Card c : ci.getCardsList()) ids.add(c.getValue());
		}
		return ids;
	}

	/** 从手牌中提取卡片（用Map索引，O(n)复杂度） */
	private static List<Card> pullFromHand(TableUser user, List<Integer> ids) {
		Map<Integer, Card> index = new HashMap<>();
		for (Card c : user.getCards()) index.put(c.getId(), c);
		List<Card> out = new ArrayList<>(ids.size());
		for (int id : ids) {
			Card found = index.remove(id);
			if (found == null) return null;
			out.add(found);
		}
		return out;
	}

	/** 广播确认 */
	private static void broadcastAck(DdzTable table, int actorUserId, GameProto.OpInfo op) {
		GameProto.AckOp msg = GameProto.AckOp.newBuilder()
				.setOp(op).setOpId(actorUserId).setOpFrom(actorUserId).build();
		table.sendTableMessage(msg, GMsg.ACK_OP);
	}
}
