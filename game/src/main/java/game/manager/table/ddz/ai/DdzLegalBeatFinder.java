package game.manager.table.ddz.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import game.manager.table.cards.Card;
import game.manager.table.ddz.DdzHand;
import game.manager.table.ddz.DdzRules;

/**
 * 枚举能压制上一手的合法牌型（在同类型、炸弹、王炸规则下）。
 */
public final class DdzLegalBeatFinder {

	private DdzLegalBeatFinder() {
	}

	public static List<DdzHand> findBeatingHands(List<Card> hand, DdzHand last) {
		List<DdzHand> out = new ArrayList<>();
		if (last == null || last.getCards().isEmpty()) {
			return out;
		}
		Set<String> seen = new HashSet<>();
		tryRocket(hand, last, out, seen);
		tryBombs(hand, last, out, seen);
		if (last.isRocket()) {
			return out;
		}

		switch (last.getType()) {
			case SINGLE:
				trySingles(hand, last, out, seen);
				break;
			case DOUBLE:
				tryPairs(hand, last, out, seen);
				break;
			case TRIPLE:
				tryTriples(hand, last, out, seen);
				break;
			case STRAIGHT:
				tryStraights(hand, last, out, seen);
				break;
			case STRAIGHT_DOUBLE:
				tryStraightDoubles(hand, last, out, seen);
				break;
			case TRIPLE_ONE:
				Combinations.forEachCombination(hand, 4, pick -> addIfBeats(pick, last, out, seen));
				break;
			case TRIPLE_DOUBLE:
				Combinations.forEachCombination(hand, 5, pick -> addIfBeats(pick, last, out, seen));
				break;
			case PLANE_ONE:
				Combinations.forEachCombination(hand, last.getStraightLen() * 4,
						pick -> addIfBeats(pick, last, out, seen));
				break;
			case PLANE_DOUBLE:
				Combinations.forEachCombination(hand, last.getStraightLen() * 5,
						pick -> addIfBeats(pick, last, out, seen));
				break;
			default:
				break;
		}
		return out;
	}

	private static void tryRocket(List<Card> hand, DdzHand last, List<DdzHand> out, Set<String> seen) {
		List<Card> js = new ArrayList<>();
		for (Card c : hand) {
			if (c.isSmallJoker() || c.isBigJoker()) {
				js.add(c);
			}
		}
		if (js.size() < 2) {
			return;
		}
		Card s = null;
		Card b = null;
		for (Card c : js) {
			if (c.isSmallJoker()) {
				s = c;
			}
			if (c.isBigJoker()) {
				b = c;
			}
		}
		if (s == null || b == null) {
			return;
		}
		addIfBeats(java.util.Arrays.asList(s, b), last, out, seen);
	}

	private static void tryBombs(List<Card> hand, DdzHand last, List<DdzHand> out, Set<String> seen) {
		Map<Integer, List<Card>> by = byRank(hand);
		for (List<Card> lst : by.values()) {
			if (lst.size() < 4) {
				continue;
			}
			List<Card> bomb = new ArrayList<>(lst.subList(0, 4));
			addIfBeats(bomb, last, out, seen);
		}
	}

	private static void trySingles(List<Card> hand, DdzHand last, List<DdzHand> out, Set<String> seen) {
		for (Card c : hand) {
			addIfBeats(Collections.singletonList(c), last, out, seen);
		}
	}

	private static void tryPairs(List<Card> hand, DdzHand last, List<DdzHand> out, Set<String> seen) {
		Map<Integer, List<Card>> by = byRank(hand);
		for (List<Card> lst : by.values()) {
			if (lst.size() < 2) {
				continue;
			}
			for (int i = 0; i < lst.size(); i++) {
				for (int j = i + 1; j < lst.size(); j++) {
					addIfBeats(java.util.Arrays.asList(lst.get(i), lst.get(j)), last, out, seen);
				}
			}
		}
	}

	private static void tryTriples(List<Card> hand, DdzHand last, List<DdzHand> out, Set<String> seen) {
		Map<Integer, List<Card>> by = byRank(hand);
		for (List<Card> lst : by.values()) {
			if (lst.size() < 3) {
				continue;
			}
			Combinations.forEachCombination(lst, 3, pick -> addIfBeats(pick, last, out, seen));
		}
	}

	private static void tryStraights(List<Card> hand, DdzHand last, List<DdzHand> out, Set<String> seen) {
		int len = last.getStraightLen();
		if (len < 5) {
			return;
		}
		List<Card> work = new ArrayList<>(hand);
		for (int start = 3; start <= 14 - len + 1; start++) {
			dfsStraight(work, new boolean[work.size()], start, len, 0, new ArrayList<>(), last, out, seen);
		}
	}

	private static void dfsStraight(List<Card> hand, boolean[] used, int startRank, int len, int depth,
			List<Card> acc, DdzHand last, List<DdzHand> out, Set<String> seen) {
		if (depth == len) {
			addIfBeats(new ArrayList<>(acc), last, out, seen);
			return;
		}
		int needRank = startRank + depth;
		if (needRank > 14) {
			return;
		}
		for (int i = 0; i < hand.size(); i++) {
			if (used[i]) {
				continue;
			}
			Card c = hand.get(i);
			if (c.getCardVal() != needRank) {
				continue;
			}
			used[i] = true;
			acc.add(c);
			dfsStraight(hand, used, startRank, len, depth + 1, acc, last, out, seen);
			acc.remove(acc.size() - 1);
			used[i] = false;
		}
	}

	private static void tryStraightDoubles(List<Card> hand, DdzHand last, List<DdzHand> out, Set<String> seen) {
		int pairs = last.getStraightLen();
		if (pairs < 3) {
			return;
		}
		List<Card> work = new ArrayList<>(hand);
		for (int start = 3; start <= 14 - pairs + 1; start++) {
			dfsStraightDouble(work, new boolean[work.size()], start, pairs, 0, new ArrayList<>(), last, out, seen);
		}
	}

	private static void dfsStraightDouble(List<Card> hand, boolean[] used, int startRank, int pairs, int depth,
			List<Card> acc, DdzHand last, List<DdzHand> out, Set<String> seen) {
		if (depth == pairs) {
			addIfBeats(new ArrayList<>(acc), last, out, seen);
			return;
		}
		int needRank = startRank + depth;
		List<Integer> idx = new ArrayList<>();
		for (int i = 0; i < hand.size(); i++) {
			if (!used[i] && hand.get(i).getCardVal() == needRank) {
				idx.add(i);
			}
		}
		if (idx.size() < 2) {
			return;
		}
		for (int a = 0; a < idx.size(); a++) {
			for (int b = a + 1; b < idx.size(); b++) {
				int ia = idx.get(a);
				int ib = idx.get(b);
				used[ia] = true;
				used[ib] = true;
				acc.add(hand.get(ia));
				acc.add(hand.get(ib));
				dfsStraightDouble(hand, used, startRank, pairs, depth + 1, acc, last, out, seen);
				acc.remove(acc.size() - 1);
				acc.remove(acc.size() - 1);
				used[ia] = false;
				used[ib] = false;
			}
		}
	}

	private static Map<Integer, List<Card>> byRank(List<Card> hand) {
		return hand.stream().collect(Collectors.groupingBy(Card::getCardVal, TreeMap::new, Collectors.toList()));
	}

	private static void addIfBeats(List<Card> cards, DdzHand last, List<DdzHand> out, Set<String> seen) {
		Optional<DdzHand> o = DdzRules.analyze(cards);
		if (!o.isPresent()) {
			return;
		}
		DdzHand h = o.get();
		if (!DdzRules.beats(h, last)) {
			return;
		}
		if (seen.add(signature(h))) {
			out.add(h);
		}
	}

	public static String signature(DdzHand h) {
		List<Integer> ids = new ArrayList<>();
		for (Card c : h.getCards()) {
			ids.add(c.getId());
		}
		Collections.sort(ids);
		return h.getType().name() + ":" + ids.toString();
	}
}
