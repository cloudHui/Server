package game.manager.table.ddz.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.cards.Card;
import game.manager.table.ddz.DdzHand;
import game.manager.table.ddz.DdzRules;
import proto.ConstProto;
import proto.GameProto;

/**
 * 简易托管 AI：拆牌规划 + 合法压制枚举 + 阶段/角色极简启发。
 */
public final class DdzSimpleAi {

	private DdzSimpleAi() {
	}

	public static GameProto.OpInfo decide(Table table, TableUser user) {
		List<Card> hand = new ArrayList<>(user.getCards());
		if (hand.isEmpty()) {
			return pass();
		}
		int phase = phaseOf(hand.size());
		DdzHand last = table.getDdz().getLastHand();

		if (last == null || last.getCards().isEmpty()) {
			return lead(hand, phase);
		}
		if (shouldPassAfterTeammate(table, user)) {
			return pass();
		}
		List<DdzHand> beats = DdzLegalBeatFinder.findBeatingHands(hand, last);
		beats = filterHeavyBeats(beats, last, phase);
		if (beats.isEmpty()) {
			return pass();
		}
		DdzHand pick = pickCheapestBeat(beats);
		return playHand(pick);
	}

	private static int phaseOf(int handSize) {
		if (handSize >= DdzAiConstants.PHASE_EARLY_MIN_CARDS) {
			return 0;
		}
		if (handSize >= DdzAiConstants.PHASE_MID_MIN_CARDS) {
			return 1;
		}
		return 2;
	}

	private static boolean shouldPassAfterTeammate(Table table, TableUser user) {
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
		return true;
	}

	private static List<DdzHand> filterHeavyBeats(List<DdzHand> beats, DdzHand last, int phase) {
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

	private static DdzHand pickCheapestBeat(List<DdzHand> beats) {
		DdzHand best = null;
		double bestCost = Double.MAX_VALUE;
		for (DdzHand h : beats) {
			double cost = h.getStrengthKey() * DdzAiConstants.FOLLOW_STRENGTH_MARGIN_PENALTY;
			if (h.isBomb()) {
				cost += DdzAiConstants.FOLLOW_BOMB_BASE_COST;
			}
			if (h.isRocket()) {
				cost += DdzAiConstants.FOLLOW_ROCKET_COST;
			}
			if (cost < bestCost) {
				bestCost = cost;
				best = h;
			}
		}
		return best;
	}

	private static GameProto.OpInfo lead(List<Card> hand, int phase) {
		List<CardGroup> plan = DdzSplitPlanner.plan(hand);
		Set<String> seen = new HashSet<>();
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

	private static void addLeadCandidate(List<Card> cards, List<DdzHand> candidates, Set<String> seen) {
		Optional<DdzHand> o = DdzRules.analyze(cards);
		if (!o.isPresent()) {
			return;
		}
		DdzHand h = o.get();
		if (seen.add(DdzLegalBeatFinder.signature(h))) {
			candidates.add(h);
		}
	}

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
			if (v >= 13 || v >= game.manager.table.card.CardConst.SMALL_JOKER_VAL) {
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

	private static GameProto.OpInfo playHand(DdzHand h) {
		return GameProto.OpInfo.newBuilder()
				.setChoice(ConstProto.Operation.PLAY)
				.addOpCards(h.toCardInfo())
				.build();
	}

	private static GameProto.OpInfo pass() {
		return GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.PASS).build();
	}
}
