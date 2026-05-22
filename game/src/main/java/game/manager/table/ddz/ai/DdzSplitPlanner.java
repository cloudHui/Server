package game.manager.table.ddz.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import game.manager.table.cards.Card;

/**
 * DdzSplitPlanner
 * 简易拆牌：火箭→炸弹→飞机带对→飞机带单→三张→顺子→连对→对子→单张。权重见 {@link DdzAiConstants}。
 * 
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 */
public final class DdzSplitPlanner {

	private DdzSplitPlanner() {
	}

	/**
	 * 计划
	 * 
	 * @param hand 手牌
	 * @return 拆牌组
	 */
	public static List<CardGroup> plan(List<Card> hand) {
		List<CardGroup> groups = new ArrayList<>();
		TreeMap<Integer, List<Card>> pool = new TreeMap<>();
		for (Card c : hand) {
			pool.computeIfAbsent(c.getCardVal(), k -> new ArrayList<>()).add(c);
		}

		extractRocket(pool, groups);
		extractBombs(pool, groups);
		extractPlaneDoubles(pool, groups);
		extractPlaneOnes(pool, groups);
		extractTriples(pool, groups);
		extractStraights(pool, groups);
		extractStraightDoubles(pool, groups);
		extractPairs(pool, groups);
		extractSingles(pool, groups);
		return groups;
	}

	/**
	 * 提取火箭
	 * 
	 * @param pool   牌池
	 * @param groups 拆牌组
	 */
	private static void extractRocket(TreeMap<Integer, List<Card>> pool, List<CardGroup> groups) {
		List<Card> sj = pool.get(game.manager.table.card.CardConst.SMALL_JOKER_VAL);
		List<Card> bj = pool.get(game.manager.table.card.CardConst.BIG_JOKER_VAL);
		if (sj != null && !sj.isEmpty() && bj != null && !bj.isEmpty()) {
			List<Card> rocket = new ArrayList<>();
			rocket.add(sj.remove(sj.size() - 1));
			rocket.add(bj.remove(bj.size() - 1));
			removeEmpty(pool, game.manager.table.card.CardConst.SMALL_JOKER_VAL);
			removeEmpty(pool, game.manager.table.card.CardConst.BIG_JOKER_VAL);
			groups.add(new CardGroup(rocket, DdzAiConstants.SPLIT_WEIGHT_ROCKET));
		}
	}

	/**
	 * 移除空牌
	 * 
	 * @param pool 牌池
	 * @param rank 排名
	 */
	private static void removeEmpty(TreeMap<Integer, List<Card>> pool, int rank) {
		List<Card> l = pool.get(rank);
		if (l != null && l.isEmpty()) {
			pool.remove(rank);
		}
	}

	/**
	 * 提取炸弹
	 * 
	 * @param pool   牌池
	 * @param groups 拆牌组
	 */
	private static void extractBombs(TreeMap<Integer, List<Card>> pool, List<CardGroup> groups) {
		boolean progress = true;
		while (progress) {
			progress = false;
			for (Map.Entry<Integer, List<Card>> e : new ArrayList<>(pool.entrySet())) {
				int r = e.getKey();
				if (r == game.manager.table.card.CardConst.SMALL_JOKER_VAL
						|| r == game.manager.table.card.CardConst.BIG_JOKER_VAL) {
					continue;
				}
				List<Card> lst = e.getValue();
				while (lst != null && lst.size() >= 4) {
					List<Card> bomb = new ArrayList<>();
					for (int i = 0; i < 4; i++) {
						bomb.add(lst.remove(lst.size() - 1));
					}
					groups.add(new CardGroup(bomb, DdzAiConstants.SPLIT_WEIGHT_BOMB));
					progress = true;
				}
				removeEmpty(pool, r);
			}
		}
	}

	/**
	 * 提取飞机带对
	 * 
	 * @param pool   牌池
	 * @param groups 拆牌组
	 */
	private static void extractPlaneDoubles(TreeMap<Integer, List<Card>> pool, List<CardGroup> groups) {
		boolean progress = true;
		while (progress) {
			progress = false;
			found: for (int k = 12; k >= 2; k--) {
				for (int start = 3; start <= 14 - k + 1; start++) {
					List<Card> taken = tryTakePlaneDouble(pool, start, k);
					if (taken != null) {
						int score = DdzAiConstants.splitPlaneGroupScore(k);
						groups.add(new CardGroup(taken, score));
						progress = true;
						break found;
					}
				}
			}
		}
	}

	/**
	 * 提取飞机带单
	 * 
	 * @param pool   牌池
	 * @param groups 拆牌组
	 */
	private static void extractPlaneOnes(TreeMap<Integer, List<Card>> pool, List<CardGroup> groups) {
		boolean progress = true;
		while (progress) {
			progress = false;
			found: for (int k = 12; k >= 2; k--) {
				for (int start = 3; start <= 14 - k + 1; start++) {
					List<Card> taken = tryTakePlaneOne(pool, start, k);
					if (taken != null) {
						int score = DdzAiConstants.splitPlaneGroupScore(k);
						groups.add(new CardGroup(taken, score));
						progress = true;
						break found;
					}
				}
			}
		}
	}

	/**
	 * 尝试提取飞机带对
	 * 
	 * @param pool  牌池
	 * @param start 开始排名
	 * @param k     飞机带对数量
	 * @return 飞机带对
	 */
	private static List<Card> tryTakePlaneDouble(TreeMap<Integer, List<Card>> pool, int start, int k) {
		if (!hasTripleBody(pool, start, k)) {
			return null;
		}
		TreeMap<Integer, List<Card>> sim = clonePool(pool);
		takeTripleBody(sim, start, k, new ArrayList<>());
		if (totalCards(sim) < 2 * k || maxPairCount(sim) < k) {
			return null;
		}
		List<Card> out = new ArrayList<>(5 * k);
		takeTripleBody(pool, start, k, out);
		takeKPairsGreedy(pool, k, out);
		return out;
	}

	/**
	 * 尝试提取飞机带单
	 * 
	 * @param pool  牌池
	 * @param start 开始排名
	 * @param k     飞机带单数量
	 * @return 飞机带单
	 */
	private static List<Card> tryTakePlaneOne(TreeMap<Integer, List<Card>> pool, int start, int k) {
		if (!hasTripleBody(pool, start, k)) {
			return null;
		}
		TreeMap<Integer, List<Card>> sim = clonePool(pool);
		takeTripleBody(sim, start, k, new ArrayList<>());
		if (totalCards(sim) < k) {
			return null;
		}
		List<Card> out = new ArrayList<>(4 * k);
		takeTripleBody(pool, start, k, out);
		takeKSinglesGreedy(pool, k, out);
		return out;
	}

	/**
	 * 克隆牌池
	 * 
	 * @param pool 牌池
	 * @return 克隆牌池
	 */
	private static TreeMap<Integer, List<Card>> clonePool(TreeMap<Integer, List<Card>> pool) {
		TreeMap<Integer, List<Card>> c = new TreeMap<>();
		for (Map.Entry<Integer, List<Card>> e : pool.entrySet()) {
			c.put(e.getKey(), new ArrayList<>(e.getValue()));
		}
		return c;
	}

	/**
	 * 计算牌池中牌的数量
	 * 
	 * @param pool 牌池
	 * @return 牌池中牌的数量
	 */
	private static int totalCards(TreeMap<Integer, List<Card>> pool) {
		int t = 0;
		for (List<Card> lst : pool.values()) {
			t += lst.size();
		}
		return t;
	}

	/**
	 * 计算牌池中对子的数量
	 * 
	 * @param pool 牌池
	 * @return 牌池中对子的数量
	 */
	private static int maxPairCount(TreeMap<Integer, List<Card>> pool) {
		int p = 0;
		for (List<Card> lst : pool.values()) {
			p += lst.size() / 2;
		}
		return p;
	}

	/**
	 * 判断牌池中是否有飞机带对
	 * 
	 * @param pool  牌池
	 * @param start 开始排名
	 * @param k     飞机带对数量
	 * @return 是否有飞机带对
	 */
	private static boolean hasTripleBody(TreeMap<Integer, List<Card>> pool, int start, int k) {
		for (int i = 0; i < k; i++) {
			int r = start + i;
			List<Card> lst = pool.get(r);
			if (lst == null || lst.size() < 3) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 提取飞机带对
	 * 
	 * @param pool  牌池
	 * @param start 开始排名
	 * @param k     飞机带对数量
	 * @param sink  拆牌组
	 */
	private static void takeTripleBody(TreeMap<Integer, List<Card>> pool, int start, int k, List<Card> sink) {
		for (int i = 0; i < k; i++) {
			int r = start + i;
			List<Card> lst = pool.get(r);
			for (int j = 0; j < 3; j++) {
				sink.add(lst.remove(lst.size() - 1));
			}
			removeEmpty(pool, r);
		}
	}

	/**
	 * 提取对子
	 * 
	 * @param pool        牌池
	 * @param pairsNeeded 对子数量
	 * @param sink        拆牌组
	 */
	private static void takeKPairsGreedy(TreeMap<Integer, List<Card>> pool, int pairsNeeded, List<Card> sink) {
		for (int done = 0; done < pairsNeeded; done++) {
			boolean moved = false;
			for (Integer rank : new ArrayList<>(pool.keySet())) {
				List<Card> lst = pool.get(rank);
				if (lst != null && lst.size() >= 2) {
					sink.add(lst.remove(lst.size() - 1));
					sink.add(lst.remove(lst.size() - 1));
					removeEmpty(pool, rank);
					moved = true;
					break;
				}
			}
			if (!moved) {
				return;
			}
		}
	}

	/**
	 * 提取单张
	 * 
	 * @param pool          牌池
	 * @param singlesNeeded 单张数量
	 * @param sink          拆牌组
	 */
	private static void takeKSinglesGreedy(TreeMap<Integer, List<Card>> pool, int singlesNeeded, List<Card> sink) {
		for (int done = 0; done < singlesNeeded; done++) {
			boolean moved = false;
			for (Integer rank : new ArrayList<>(pool.keySet())) {
				List<Card> lst = pool.get(rank);
				if (lst != null && !lst.isEmpty()) {
					sink.add(lst.remove(lst.size() - 1));
					removeEmpty(pool, rank);
					moved = true;
					break;
				}
			}
			if (!moved) {
				return;
			}
		}
	}

	/**
	 * 提取三张
	 * 
	 * @param pool   牌池
	 * @param groups 拆牌组
	 */
	private static void extractTriples(TreeMap<Integer, List<Card>> pool, List<CardGroup> groups) {
		for (Map.Entry<Integer, List<Card>> e : new ArrayList<>(pool.entrySet())) {
			int r = e.getKey();
			List<Card> lst = e.getValue();
			while (lst != null && lst.size() >= 3) {
				List<Card> t = new ArrayList<>();
				for (int kk = 0; kk < 3; kk++) {
					t.add(lst.remove(lst.size() - 1));
				}
				groups.add(new CardGroup(t, DdzAiConstants.SPLIT_WEIGHT_TRIPLE));
			}
			removeEmpty(pool, r);
		}
	}

	/**
	 * 提取顺子
	 * 
	 * @param pool   牌池
	 * @param groups 拆牌组
	 */
	private static void extractStraights(TreeMap<Integer, List<Card>> pool, List<CardGroup> groups) {
		boolean progress = true;
		while (progress) {
			progress = false;
			for (int len = 12; len >= 5 && !progress; len--) {
				for (int start = 3; start <= 14 - len + 1 && !progress; start++) {
					if (canTakeStraight(pool, start, len)) {
						List<Card> straight = takeStraight(pool, start, len);
						int score = DdzAiConstants.SPLIT_WEIGHT_STRAIGHT_MIN_BONUS
								+ len * DdzAiConstants.SPLIT_WEIGHT_STRAIGHT_PER_CARD;
						groups.add(new CardGroup(straight, score));
						progress = true;
					}
				}
			}
		}
	}

	/**
	 * 判断是否可以提取顺子
	 * 
	 * @param pool  牌池
	 * @param start 开始排名
	 * @param len   顺子长度
	 * @return 是否可以提取顺子
	 */
	private static boolean canTakeStraight(TreeMap<Integer, List<Card>> pool, int start, int len) {
		for (int r = start; r < start + len; r++) {
			if (!isStraightRank(r)) {
				return false;
			}
			List<Card> lst = pool.get(r);
			if (lst == null || lst.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 提取顺子
	 * 
	 * @param pool  牌池
	 * @param start 开始排名
	 * @param len   顺子长度
	 * @return 顺子
	 */
	private static List<Card> takeStraight(TreeMap<Integer, List<Card>> pool, int start, int len) {
		List<Card> straight = new ArrayList<>();
		for (int r = start; r < start + len; r++) {
			List<Card> lst = pool.get(r);
			straight.add(lst.remove(lst.size() - 1));
			removeEmpty(pool, r);
		}
		return straight;
	}

	/**
	 * 判断是否是顺子排名
	 * 
	 * @param r 排名
	 * @return 是否是顺子排名
	 */
	private static boolean isStraightRank(int r) {
		return r >= 3 && r <= 14;
	}

	/**
	 * 提取双顺子
	 * 
	 * @param pool   牌池
	 * @param groups 拆牌组
	 */
	private static void extractStraightDoubles(TreeMap<Integer, List<Card>> pool, List<CardGroup> groups) {
		boolean progress = true;
		while (progress) {
			progress = false;
			for (int pairs = 8; pairs >= 3 && !progress; pairs--) {
				for (int start = 3; start <= 14 - pairs + 1 && !progress; start++) {
					if (canTakeStraightPair(pool, start, pairs)) {
						List<Card> sd = takeStraightPair(pool, start, pairs);
						int score = DdzAiConstants.SPLIT_WEIGHT_STRAIGHT_DOUBLE_MIN_BONUS
								+ pairs * DdzAiConstants.SPLIT_WEIGHT_STRAIGHT_DOUBLE_PER_PAIR;
						groups.add(new CardGroup(sd, score));
						progress = true;
					}
				}
			}
		}
	}

	/**
	 * 判断是否可以提取双顺子
	 * 
	 * @param pool  牌池
	 * @param start 开始排名
	 * @param pairs 双顺子数量
	 * @return 是否可以提取双顺子
	 */
	private static boolean canTakeStraightPair(TreeMap<Integer, List<Card>> pool, int start, int pairs) {
		for (int i = 0; i < pairs; i++) {
			int r = start + i;
			if (!isStraightRank(r)) {
				return false;
			}
			List<Card> lst = pool.get(r);
			if (lst == null || lst.size() < 2) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 提取双顺子
	 * 
	 * @param pool  牌池
	 * @param start 开始排名
	 * @param pairs 双顺子数量
	 * @return 双顺子
	 */
	private static List<Card> takeStraightPair(TreeMap<Integer, List<Card>> pool, int start, int pairs) {
		List<Card> out = new ArrayList<>();
		for (int i = 0; i < pairs; i++) {
			int r = start + i;
			List<Card> lst = pool.get(r);
			out.add(lst.remove(lst.size() - 1));
			out.add(lst.remove(lst.size() - 1));
			removeEmpty(pool, r);
		}
		return out;
	}

	/**
	 * 提取对子
	 * 
	 * @param pool   牌池
	 * @param groups 拆牌组
	 */
	private static void extractPairs(TreeMap<Integer, List<Card>> pool, List<CardGroup> groups) {
		for (Map.Entry<Integer, List<Card>> e : new ArrayList<>(pool.entrySet())) {
			List<Card> lst = e.getValue();
			while (lst != null && lst.size() >= 2) {
				List<Card> p = new ArrayList<>();
				p.add(lst.remove(lst.size() - 1));
				p.add(lst.remove(lst.size() - 1));
				groups.add(new CardGroup(p, DdzAiConstants.SPLIT_WEIGHT_PAIR));
			}
			removeEmpty(pool, e.getKey());
		}
	}

	/**
	 * 提取单张
	 * 
	 * @param pool   牌池
	 * @param groups 拆牌组
	 */
	private static void extractSingles(TreeMap<Integer, List<Card>> pool, List<CardGroup> groups) {
		for (Map.Entry<Integer, List<Card>> e : new ArrayList<>(pool.entrySet())) {
			List<Card> lst = e.getValue();
			while (lst != null && !lst.isEmpty()) {
				List<Card> s = new ArrayList<>();
				s.add(lst.remove(lst.size() - 1));
				int w = DdzAiConstants.SPLIT_WEIGHT_SINGLE + DdzAiConstants.splitSingleExtra(e.getKey());
				groups.add(new CardGroup(s, w));
			}
			removeEmpty(pool, e.getKey());
		}
	}
}
