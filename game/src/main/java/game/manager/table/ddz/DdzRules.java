package game.manager.table.ddz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import game.manager.table.cards.Card;
import proto.ConstProto;

/**
 * 斗地主出牌规则：识别牌型、比大小。
 * <p>
 * 支持：单张、对子、三张、三带一、三带二、顺子(≥5)、连对(≥3对)、飞机带单、飞机带对、炸弹、王炸。
 * 飞机机身为点数 3～A 的连续若干组三张（每组恰好取三张），不带 2 与王；比较时同类飞机须段数相同，比机身最大点数。
 */
public final class DdzRules {

	private DdzRules() {
	}

	public static Optional<DdzHand> analyze(List<Card> cards) {
		if (cards == null || cards.isEmpty()) {
			return Optional.empty();
		}
		List<Card> sorted = new ArrayList<>(cards);
		sorted.sort(Collections.reverseOrder());

		int n = sorted.size();
		Map<Integer, Long> cnt = sorted.stream().collect(Collectors.groupingBy(Card::getCardVal, TreeMap::new, Collectors.counting()));

		if (n == 2) {
			Card a = sorted.get(0);
			Card b = sorted.get(1);
			boolean rocket = a.isSmallJoker() && b.isBigJoker() || a.isBigJoker() && b.isSmallJoker();
			if (rocket) {
				return Optional.of(new DdzHand(ConstProto.CardType.BOOM_MAX, sorted, true, false, 10000, 0));
			}
		}

		Optional<DdzHand> bomb = tryBomb(sorted, cnt);
		if (bomb.isPresent()) {
			return bomb;
		}

		if (cnt.size() == 1 && n == 1) {
			int v = sorted.get(0).getCardVal();
			return Optional.of(new DdzHand(ConstProto.CardType.SINGLE, sorted, false, false, singleStrength(v), 0));
		}
		if (cnt.size() == 1 && n == 2) {
			int v = sorted.get(0).getCardVal();
			return Optional.of(new DdzHand(ConstProto.CardType.DOUBLE, sorted, false, false, pairStrength(v), 0));
		}
		if (cnt.size() == 1 && n == 3) {
			int v = sorted.get(0).getCardVal();
			return Optional.of(new DdzHand(ConstProto.CardType.TRIPLE, sorted, false, false, tripleStrength(v), 0));
		}

		Optional<DdzHand> straight = tryStraight(sorted, cnt);
		if (straight.isPresent()) {
			return straight;
		}

		Optional<DdzHand> straightPair = tryStraightPairs(sorted, cnt);
		if (straightPair.isPresent()) {
			return straightPair;
		}

		Optional<DdzHand> planeDouble = tryPlaneDouble(cnt, sorted);
		if (planeDouble.isPresent()) {
			return planeDouble;
		}

		Optional<DdzHand> planeOne = tryPlaneOne(cnt, sorted);
		if (planeOne.isPresent()) {
			return planeOne;
		}

		Optional<DdzHand> t1 = tryTripleSingle(cnt, sorted);
		if (t1.isPresent()) {
			return t1;
		}

		return tryTriplePair(cnt, sorted);
	}

	public static boolean beats(DdzHand incoming, DdzHand last) {
		if (incoming == null) {
			return false;
		}
		if (incoming.isRocket()) {
			return true;
		}
		if (last == null || last.getCards().isEmpty()) {
			return true;
		}
		if (last.isRocket()) {
			return false;
		}
		if (incoming.isBomb()) {
			if (!last.isBomb()) {
				return true;
			}
			return incoming.getStrengthKey() > last.getStrengthKey();
		}
		if (last.isBomb()) {
			return false;
		}
		if (incoming.getType() != last.getType()) {
			return false;
		}
		if (incoming.getType() == ConstProto.CardType.STRAIGHT && incoming.getStraightLen() != last.getStraightLen()) {
			return false;
		}
		if (incoming.getType() == ConstProto.CardType.STRAIGHT_DOUBLE && incoming.getStraightLen() != last.getStraightLen()) {
			return false;
		}
		if (incoming.getType() == ConstProto.CardType.PLANE_ONE && last.getType() == ConstProto.CardType.PLANE_ONE) {
			if (incoming.getStraightLen() != last.getStraightLen()) {
				return false;
			}
			return incoming.getStrengthKey() > last.getStrengthKey();
		}
		if (incoming.getType() == ConstProto.CardType.PLANE_DOUBLE && last.getType() == ConstProto.CardType.PLANE_DOUBLE) {
			if (incoming.getStraightLen() != last.getStraightLen()) {
				return false;
			}
			return incoming.getStrengthKey() > last.getStrengthKey();
		}
		return incoming.getStrengthKey() > last.getStrengthKey();
	}

	private static Optional<DdzHand> tryBomb(List<Card> sorted, Map<Integer, Long> cnt) {
		if (sorted.size() != 4) {
			return Optional.empty();
		}
		if (cnt.size() != 1) {
			return Optional.empty();
		}
		Map.Entry<Integer, Long> e = cnt.entrySet().iterator().next();
		if (e.getValue() != 4) {
			return Optional.empty();
		}
		int v = e.getKey();
		return Optional.of(new DdzHand(ConstProto.CardType.BOOM, sorted, false, true, bombStrength(v), 0));
	}

	private static Optional<DdzHand> tryStraight(List<Card> sorted, Map<Integer, Long> cnt) {
		int n = sorted.size();
		if (n < 5) {
			return Optional.empty();
		}
		for (Long c : cnt.values()) {
			if (c != 1) {
				return Optional.empty();
			}
		}
		List<Integer> ranks = new ArrayList<>(cnt.keySet());
		Collections.sort(ranks);
		for (int r : ranks) {
			if (!isStraightEligibleRank(r)) {
				return Optional.empty();
			}
		}
		for (int i = 1; i < ranks.size(); i++) {
			if (ranks.get(i) != ranks.get(i - 1) + 1) {
				return Optional.empty();
			}
		}
		int top = ranks.get(ranks.size() - 1);
		return Optional.of(new DdzHand(ConstProto.CardType.STRAIGHT, sorted, false, false, straightStrength(top), n));
	}

	private static Optional<DdzHand> tryStraightPairs(List<Card> sorted, Map<Integer, Long> cnt) {
		int n = sorted.size();
		if (n < 6 || n % 2 != 0) {
			return Optional.empty();
		}
		for (Long c : cnt.values()) {
			if (c != 2) {
				return Optional.empty();
			}
		}
		int pairs = n / 2;
		if (pairs < 3) {
			return Optional.empty();
		}
		List<Integer> ranks = new ArrayList<>(cnt.keySet());
		Collections.sort(ranks);
		for (int r : ranks) {
			if (!isStraightEligibleRank(r)) {
				return Optional.empty();
			}
		}
		for (int i = 1; i < ranks.size(); i++) {
			if (ranks.get(i) != ranks.get(i - 1) + 1) {
				return Optional.empty();
			}
		}
		int top = ranks.get(ranks.size() - 1);
		return Optional.of(new DdzHand(ConstProto.CardType.STRAIGHT_DOUBLE, sorted, false, false, straightStrength(top), pairs));
	}

	/**
	 * 飞机带对：k 组连续三张（3～A）+ k 对翅膀，共 5k 张，k≥2。
	 */
	private static Optional<DdzHand> tryPlaneDouble(Map<Integer, Long> cnt, List<Card> sorted) {
		int n = sorted.size();
		if (n < 10 || n % 5 != 0) {
			return Optional.empty();
		}
		int k = n / 5;
		if (k < 2) {
			return Optional.empty();
		}
		for (int start = 3; start <= 14 - k + 1; start++) {
			Map<Integer, Long> rem = remainderAfterPlaneBody(cnt, start, k);
			if (rem == null) {
				continue;
			}
			if (!planeDoubleWingsValid(rem, k)) {
				continue;
			}
			int topBodyRank = start + k - 1;
			return Optional.of(new DdzHand(ConstProto.CardType.PLANE_DOUBLE, sorted, false, false, tripleStrength(topBodyRank), k));
		}
		return Optional.empty();
	}

	/**
	 * 飞机带单：k 组连续三张（3～A）+ k 张单翅膀，共 4k 张，k≥2。
	 */
	private static Optional<DdzHand> tryPlaneOne(Map<Integer, Long> cnt, List<Card> sorted) {
		int n = sorted.size();
		if (n < 8 || n % 4 != 0) {
			return Optional.empty();
		}
		int k = n / 4;
		if (k < 2) {
			return Optional.empty();
		}
		for (int start = 3; start <= 14 - k + 1; start++) {
			Map<Integer, Long> rem = remainderAfterPlaneBody(cnt, start, k);
			if (rem == null) {
				continue;
			}
			if (sumCounts(rem) != k) {
				continue;
			}
			int topBodyRank = start + k - 1;
			return Optional.of(new DdzHand(ConstProto.CardType.PLANE_ONE, sorted, false, false, tripleStrength(topBodyRank), k));
		}
		return Optional.empty();
	}

	/** 从计数里减去机身（连续 k 个名次各取三张），失败返回 null */
	private static Map<Integer, Long> remainderAfterPlaneBody(Map<Integer, Long> cnt, int start, int k) {
		TreeMap<Integer, Long> rem = new TreeMap<>(cnt);
		for (int i = 0; i < k; i++) {
			int r = start + i;
			Long v = rem.get(r);
			if (v == null || v < 3) {
				return null;
			}
			long nv = v - 3;
			if (nv == 0) {
				rem.remove(r);
			} else {
				rem.put(r, nv);
			}
		}
		return rem;
	}

	private static long sumCounts(Map<Integer, Long> m) {
		long s = 0;
		for (Long v : m.values()) {
			s += v;
		}
		return s;
	}

	private static boolean planeDoubleWingsValid(Map<Integer, Long> rem, int k) {
		if (sumCounts(rem) != 2L * k) {
			return false;
		}
		for (Long v : rem.values()) {
			if ((v & 1) != 0) {
				return false;
			}
		}
		return true;
	}

	private static Optional<DdzHand> tryTripleSingle(Map<Integer, Long> cnt, List<Card> sorted) {
		if (sorted.size() != 4 || cnt.size() != 2) {
			return Optional.empty();
		}
		int tripleRank = -1;
		int singleRank = -1;
		for (Map.Entry<Integer, Long> e : cnt.entrySet()) {
			if (e.getValue() == 3) {
				tripleRank = e.getKey();
			} else if (e.getValue() == 1) {
				singleRank = e.getKey();
			} else {
				return Optional.empty();
			}
		}
		if (tripleRank < 0 || singleRank < 0) {
			return Optional.empty();
		}
		return Optional.of(new DdzHand(ConstProto.CardType.TRIPLE_ONE, sorted, false, false, tripleStrength(tripleRank), 0));
	}

	private static Optional<DdzHand> tryTriplePair(Map<Integer, Long> cnt, List<Card> sorted) {
		if (sorted.size() != 5 || cnt.size() != 2) {
			return Optional.empty();
		}
		int tripleRank = -1;
		int pairRank = -1;
		for (Map.Entry<Integer, Long> e : cnt.entrySet()) {
			if (e.getValue() == 3) {
				tripleRank = e.getKey();
			} else if (e.getValue() == 2) {
				pairRank = e.getKey();
			} else {
				return Optional.empty();
			}
		}
		if (tripleRank < 0 || pairRank < 0) {
			return Optional.empty();
		}
		return Optional.of(new DdzHand(ConstProto.CardType.TRIPLE_DOUBLE, sorted, false, false, tripleStrength(tripleRank), 0));
	}

	private static boolean isStraightEligibleRank(int cardVal) {
		return cardVal >= 3 && cardVal <= 14;
	}

	private static int singleStrength(int cardVal) {
		return normalizePoint(cardVal);
	}

	private static int pairStrength(int cardVal) {
		return normalizePoint(cardVal);
	}

	private static int tripleStrength(int cardVal) {
		return normalizePoint(cardVal);
	}

	private static int bombStrength(int cardVal) {
		return normalizePoint(cardVal) + 1000;
	}

	private static int straightStrength(int topRank) {
		return normalizePoint(topRank);
	}

	/** 3 最小，2 仅次于王；小王、大王最高（用于非火箭单牌比较时的扩展，此处炸弹/火箭已单列） */
	private static int normalizePoint(int cardVal) {
		if (cardVal >= 3 && cardVal <= 13) {
			return cardVal - 3;
		}
		if (cardVal == 14) {
			return 11;
		}
		if (cardVal == 15) {
			return 12;
		}
		if (cardVal == CardConstBridge.smallJoker()) {
			return 13;
		}
		if (cardVal == CardConstBridge.bigJoker()) {
			return 14;
		}
		return cardVal;
	}

	/** 避免直接依赖 CardConst 命名冲突的小桥接 */
	private static final class CardConstBridge {
		static int smallJoker() {
			return game.manager.table.card.CardConst.SMALL_JOKER_VAL;
		}

		static int bigJoker() {
			return game.manager.table.card.CardConst.BIG_JOKER_VAL;
		}
	}
}
