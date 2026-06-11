package game.manager.table.ddz.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import game.manager.table.DdzTable;
import game.manager.table.TableUser;
import game.manager.table.card.CardConst;
import game.manager.table.cards.Card;
import game.manager.table.ddz.DdzHand;
import game.manager.table.ddz.DdzRules;
import proto.ConstProto;
import proto.GameProto;

/**
 * DdzSimpleAi
 * 简易托管 AI：拆牌规划 + 合法压制枚举 + 阶段/角色极简启发。
 * 
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 */
public final class DdzSimpleAi {

	private DdzSimpleAi() {
	}

	/**
	 * 决定
	 * 
	 * @param table 桌子
	 * @param user  用户
	 * @return 决定
	 */
	public static GameProto.OpInfo decide(DdzTable table, TableUser user) {
		List<Card> hand = new ArrayList<>(user.getCards());
		if (hand.isEmpty()) {
			return pass();
		}
		int visionLevel = table.getDdz().getVisionLevel();
		int aiLevel = table.getDdz().getAiLevel();
		DdzVision vision = new DdzVision(table, user, visionLevel, aiLevel);

		// 最低级 AI：无策略，出最小单牌或直接过
		if (aiLevel == AiVision.AI_DUMB) {
			return decideDumb(hand, table);
		}

		int phase = phaseOf(hand.size());
		DdzHand last = table.getDdz().getLastHand();

		if (last == null || last.getCards().isEmpty()) {
			return lead(hand, phase, vision);
		}
		if (shouldPassAfterTeammate(table, user, vision)) {
			return pass();
		}
		int minOppCards = vision.getMinOpponentCards();
		List<DdzHand> beats = DdzLegalBeatFinder.findBeatingHands(hand, last);
		beats = filterHeavyBeats(beats, last, phase, minOppCards);
		if (beats.isEmpty()) {
			return pass();
		}
		DdzHand pick = pickCheapestBeat(beats, minOppCards);
		return playHand(pick);
	}

	/**
	 * 最低级 AI：无策略决策。
	 * 首出 → 出最小的一手（拆牌后取第一个组），跟牌 → 直接 pass。
	 */
	private static GameProto.OpInfo decideDumb(List<Card> hand, DdzTable table) {
		DdzHand last = table.getDdz().getLastHand();
		// 跟牌：直接过
		if (last != null && !last.getCards().isEmpty()) {
			return pass();
		}
		// 首出：出拆牌的第一组（最小的）
		List<CardGroup> plan = DdzSplitPlanner.plan(hand);
		if (!plan.isEmpty()) {
			Optional<DdzHand> o = DdzRules.analyze(plan.get(0).getCards());
			if (o.isPresent()) {
				return playHand(o.get());
			}
		}
		// fallback: 出最小单牌
		Card c = Collections.min(hand);
		Optional<DdzHand> one = DdzRules.analyze(Collections.singletonList(c));
		return one.map(DdzSimpleAi::playHand).orElseGet(DdzSimpleAi::pass);
	}

	/**
	 * 阶段
	 * 
	 * @param handSize 手牌大小
	 * @return 阶段
	 */
	private static int phaseOf(int handSize) {
		if (handSize >= DdzAiConstants.PHASE_EARLY_MIN_CARDS) {
			return 0;
		}
		if (handSize >= DdzAiConstants.PHASE_MID_MIN_CARDS) {
			return 1;
		}
		return 2;
	}

	/**
	 * 是否应该在队友出牌后 PASS（农民智能配合）
	 * <p>
	 * 以下情况不 PASS：
	 * 1. 队友出的牌很弱（strengthKey < 阈值），主动接过来控场
	 * 2. 地主手牌 ≤ DANGER_CARDS，应该压制不让地主跑
	 * 3. 自己手牌能一手出完，直接走
	 *
	 * @param table 桌子
	 * @param user  用户
	 * @return 是否应该 PASS
	 */
	private static boolean shouldPassAfterTeammate(DdzTable table, TableUser user, DdzVision vision) {
		if (!DdzAiConstants.AI_PASS_AFTER_TEAMMATE_PLAY) {
			return false;
		}
		int landlordSeat = table.getDdz().getLandlordSeat();
		int mySeat = user.getSeated();
		if (landlordSeat < 0 || mySeat == landlordSeat) {
			return false;
		}
		int lastSeat = table.getDdz().getLastPlaySeat();
		if (lastSeat < 0 || lastSeat == mySeat || lastSeat == landlordSeat) {
			return false;
		}

		// 自己能一手出完 → 不让
		if (user.getCards().size() <= 5) {
			DdzHand whole = DdzRules.analyze(new ArrayList<>(user.getCards())).orElse(null);
			if (whole != null) {
				return false;
			}
		}

		// 地主手牌危险（≤3张）→ 不让，必须压制
		if (vision.getMinOpponentCards() <= DdzAiConstants.FARMER_DANGER_LANDLORD_CARDS) {
			return false;
		}

		// 队友出的牌太弱 → 不让，主动接
		DdzHand lastHand = table.getDdz().getLastHand();
		if (lastHand != null && lastHand.getStrengthKey() < DdzAiConstants.FARMER_TEAMMATE_WEAK_THRESHOLD) {
			return false;
		}

		return true;
	}

	/**
	 * 过滤重牌
	 * 
	 * @param beats 牌
	 * @param last  上一手
	 * @param phase 阶段
	 * @return 过滤重牌
	 */
	private static List<DdzHand> filterHeavyBeats(List<DdzHand> beats, DdzHand last, int phase, int minOppCards) {
		// 对手快赢时不过滤炸弹
		if (minOppCards <= DdzAiConstants.FOLLOW_BOMB_DANGER_OPP_CARDS) {
			return beats;
		}
		if (phase > 0 || last.isBomb() || last.isRocket()) {
			return beats;
		}
		if (last.getStrengthKey() > DdzAiConstants.FOLLOW_SOFT_LAST_STRENGTH_MAX) {
			return beats;
		}
		List<DdzHand> light = new ArrayList<>();
		for (DdzHand h : beats) {
			if (!h.isBomb() && !h.isRocket()) {
				light.add(h);
			}
		}
		return light.isEmpty() ? beats : light;
	}

	/**
	 * 选择最便宜的牌
	 * 
	 * @param beats 牌
	 * @return 最便宜的牌
	 */
	private static DdzHand pickCheapestBeat(List<DdzHand> beats, int minOppCards) {
		DdzHand best = null;
		double bestCost = Double.MAX_VALUE;
		boolean danger = minOppCards <= DdzAiConstants.FOLLOW_BOMB_DANGER_OPP_CARDS;
		for (DdzHand h : beats) {
			double cost = h.getStrengthKey() * DdzAiConstants.FOLLOW_STRENGTH_MARGIN_PENALTY;
			if (h.isBomb()) {
				cost += danger ? DdzAiConstants.FOLLOW_BOMB_BASE_COST * DdzAiConstants.FOLLOW_BOMB_DANGER_DISCOUNT
						: DdzAiConstants.FOLLOW_BOMB_BASE_COST;
			}
			if (h.isRocket()) {
				cost += danger ? DdzAiConstants.FOLLOW_ROCKET_COST * DdzAiConstants.FOLLOW_BOMB_DANGER_DISCOUNT
						: DdzAiConstants.FOLLOW_ROCKET_COST;
			}
			if (cost < bestCost) {
				bestCost = cost;
				best = h;
			}
		}
		return best;
	}

	/**
	 * 出牌
	 * 
	 * @param hand  手牌
	 * @param phase 阶段
	 * @return 出牌
	 */
	private static GameProto.OpInfo lead(List<Card> hand, int phase, DdzVision vision) {
		// 终局搜索：手牌≤5时穷举最快出完方案
		if (hand.size() <= DdzAiConstants.PHASE_ENDGAME_MAX_CARDS) {
			DdzHand endgame = endgameSolve(hand);
			if (endgame != null) {
				return playHand(endgame);
			}
		}

		List<CardGroup> plan = DdzSplitPlanner.planBest(hand);
		Set<Long> seen = new HashSet<>();
		List<DdzHand> candidates = new ArrayList<>();
		for (CardGroup g : plan) {
			addLeadCandidate(g.getCards(), candidates, seen);
		}
		for (Card c : hand) {
			addLeadCandidate(Collections.singletonList(c), candidates, seen);
		}
		DdzHand best = null;
		double bestScore = Double.MAX_VALUE;
		for (DdzHand h : candidates) {
			double sc = scoreLead(h, phase);
			sc += DdzAiConstants.LEAD_PRESERVE_WEIGHT_SCALE * preserveHint(h);
			sc += DdzAiConstants.LEAD_RESIDUAL_GROUP_PENALTY * residualGroupCount(hand, h);
			if (sc < bestScore) {
				bestScore = sc;
				best = h;
			}
		}
		if (best == null) {
			Card c = Collections.min(hand);
			Optional<DdzHand> one = DdzRules.analyze(Collections.singletonList(c));
			return one.map(DdzSimpleAi::playHand).orElseGet(DdzSimpleAi::pass);
		}
		return playHand(best);
	}

	/**
	 * 出牌后剩余手牌的拆牌组数（越少越好）
	 */
	private static int residualGroupCount(List<Card> hand, DdzHand play) {
		List<Card> remaining = new ArrayList<>(hand);
		for (Card c : play.getCards()) {
			remaining.remove(c);
		}
		if (remaining.isEmpty()) {
			return 0;
		}
		return DdzSplitPlanner.plan(remaining).size();
	}

	/**
	 * 添加出牌候选
	 * 
	 * @param cards      牌
	 * @param candidates 候选
	 * @param seen       已见过
	 */
	private static void addLeadCandidate(List<Card> cards, List<DdzHand> candidates, Set<Long> seen) {
		Optional<DdzHand> o = DdzRules.analyze(cards);
		if (!o.isPresent()) {
			return;
		}
		DdzHand h = o.get();
		if (seen.add(DdzLegalBeatFinder.hashHand(h))) {
			candidates.add(h);
		}
	}

	/**
	 * 保留提示
	 * 
	 * @param h 牌型
	 * @return 保留提示
	 */
	private static double preserveHint(DdzHand h) {
		if (h.isRocket()) {
			return DdzAiConstants.SPLIT_WEIGHT_ROCKET;
		}
		if (h.isBomb()) {
			return DdzAiConstants.SPLIT_WEIGHT_BOMB;
		}
		int n = h.getCards().size();
		switch (h.getType()) {
			case STRAIGHT:
				return DdzAiConstants.SPLIT_WEIGHT_STRAIGHT_MIN_BONUS
						+ n * DdzAiConstants.SPLIT_WEIGHT_STRAIGHT_PER_CARD;
			case STRAIGHT_DOUBLE:
				return DdzAiConstants.SPLIT_WEIGHT_STRAIGHT_DOUBLE_MIN_BONUS
						+ h.getStraightLen() * DdzAiConstants.SPLIT_WEIGHT_STRAIGHT_DOUBLE_PER_PAIR;
			case TRIPLE:
			case TRIPLE_ONE:
			case TRIPLE_DOUBLE:
				return DdzAiConstants.SPLIT_WEIGHT_TRIPLE;
			case PLANE_ONE:
			case PLANE_DOUBLE:
				return DdzAiConstants.splitPlaneGroupScore(h.getStraightLen());
			case DOUBLE:
				return DdzAiConstants.SPLIT_WEIGHT_PAIR;
			default:
				return DdzAiConstants.SPLIT_WEIGHT_SINGLE;
		}
	}

	/**
	 * 得分提示
	 * 
	 * @param h     牌型
	 * @param phase 阶段
	 * @return 得分提示
	 */
	private static double scoreLead(DdzHand h, int phase) {
		double s = h.getStrengthKey();
		if (phase == 0) {
			if (h.isRocket()) {
				s += DdzAiConstants.LEAD_PENALTY_ROCKET_EARLY;
			}
			if (h.isBomb()) {
				s += DdzAiConstants.LEAD_PENALTY_BOMB_EARLY;
			}
		}
		if (h.getType() == ConstProto.CardType.SINGLE) {
			int v = h.getCards().get(0).getCardVal();
			if (v >= 7 && v <= 10) {
				s += DdzAiConstants.LEAD_BONUS_SINGLE_RANK_7_TO_10;
			}
			if (v >= 13 || v >= CardConst.SMALL_JOKER_VAL) {
				s += DdzAiConstants.LEAD_PENALTY_SINGLE_HIGH;
			}
		}
		if (h.getType() == ConstProto.CardType.DOUBLE) {
			int v = h.getCards().get(0).getCardVal();
			if (v <= 10) {
				s += DdzAiConstants.LEAD_BONUS_SMALL_PAIR_LOW_RANK;
			}
			if (v >= 13) {
				s += DdzAiConstants.LEAD_PENALTY_PAIR_HIGH_RANK;
			}
		}
		return s;
	}

	/**
	 * 出牌
	 * 
	 * @param h 牌型
	 * @return 出牌
	 */
	private static GameProto.OpInfo playHand(DdzHand h) {
		return GameProto.OpInfo.newBuilder()
				.setChoice(ConstProto.Operation.PLAY)
				.addOpCards(h.toCardInfo())
				.build();
	}

	/**
	 * 终局搜索：手牌≤5时，BFS穷举最少轮次出完方案。
	 * 返回第一手应该出的牌型，无法出完返回 null。
	 */
	private static DdzHand endgameSolve(List<Card> hand) {
		if (hand.isEmpty()) {
			return null;
		}
		// 一手出完
		Optional<DdzHand> whole = DdzRules.analyze(hand);
		if (whole.isPresent()) {
			return whole.get();
		}
		// 枚举所有合法首手，找最快出完的
		List<DdzHand> allPlays = enumerateAllPlays(hand);
		DdzHand bestFirst = null;
		int bestPlays = Integer.MAX_VALUE;
		for (DdzHand play : allPlays) {
			List<Card> remaining = removeCards(hand, play.getCards());
			int plays = endgameMinPlays(remaining, 0, bestPlays - 1);
			if (plays < bestPlays) {
				bestPlays = plays;
				bestFirst = play;
				if (bestPlays == 1) {
					break; // 再一手就出完，已最优
				}
			}
		}
		return bestFirst;
	}

	/**
	 * 终局递归：返回 remaining 出完所需的最少手数，超过 cutoff 直接返回
	 */
	private static int endgameMinPlays(List<Card> remaining, int depth, int cutoff) {
		if (remaining.isEmpty()) {
			return depth;
		}
		if (depth >= cutoff) {
			return cutoff + 1; // 剪枝
		}
		// 一手出完
		if (DdzRules.analyze(remaining).isPresent()) {
			return depth + 1;
		}
		List<DdzHand> plays = enumerateAllPlays(remaining);
		int best = cutoff + 1;
		for (DdzHand play : plays) {
			List<Card> next = removeCards(remaining, play.getCards());
			int r = endgameMinPlays(next, depth + 1, best - 1);
			if (r < best) {
				best = r;
				if (best <= depth + 1) {
					break;
				}
			}
		}
		return best;
	}

	/**
	 * 枚举手牌中所有合法的出牌（所有非空子集的合法牌型）
	 */
	private static List<DdzHand> enumerateAllPlays(List<Card> hand) {
		List<DdzHand> result = new ArrayList<>();
		Set<Long> seen = new HashSet<>();
		int n = hand.size();
		// 枚举所有非空子集 (bitmask)
		for (int mask = 1; mask < (1 << n); mask++) {
			List<Card> subset = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				if ((mask & (1 << i)) != 0) {
					subset.add(hand.get(i));
				}
			}
			Optional<DdzHand> o = DdzRules.analyze(subset);
			if (o.isPresent() && seen.add(DdzLegalBeatFinder.hashHand(o.get()))) {
				result.add(o.get());
			}
		}
		return result;
	}

	private static List<Card> removeCards(List<Card> hand, List<Card> toRemove) {
		List<Card> remaining = new ArrayList<>(hand);
		for (Card c : toRemove) {
			remaining.remove(c);
		}
		return remaining;
	}

	/**
	 * 过牌
	 * 
	 * @return 过牌
	 */
	private static GameProto.OpInfo pass() {
		return GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.PASS).build();
	}
}
