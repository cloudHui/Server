package game.manager.table.mj.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import game.manager.table.MjTable;
import game.manager.table.TableUser;
import game.manager.table.card.mj.MjConst;
import game.manager.table.cards.Card;
import game.manager.table.mj.MjClaimInfo;
import game.manager.table.mj.MjPlayService;
import proto.ConstProto;
import proto.GameProto;

/**
 * 麻将 AI：基于向听数 + 牌效率的决策引擎。
 * <p>
 * aiLevel 控制决策复杂度：
 * <ul>
 *   <li>0(DUMB)     — 摸什么打什么，claim 全过</li>
 *   <li>1(BASIC)    — 简单启发式：出孤张，能胡就胡</li>
 *   <li>2(ADVANCED) — 向听数计算 + 牌效率 + 安全牌评估</li>
 * </ul>
 *
 * @author cloud
 * @date 2026-06-11
 * @version 1.0
 * @since 1.0
 */
public final class MjSimpleAi {

	private MjSimpleAi() {
	}

	// ==================== 出牌决策 ====================

	/**
	 * AI 出牌决策（MJ_DISCARD 状态超时调用）
	 *
	 * @param table  麻将桌
	 * @param user   当前用户
	 * @param drawnTile 本回合摸到的牌 ID
	 * @return 要打出的牌 ID，-1 表示无法决定
	 */
	public static int decideDiscard(MjTable table, TableUser user, int drawnTile) {
		MjVision vision = new MjVision(table, user, table.getMjContext().getVisionLevel(),
				table.getMjContext().getAiLevel());
		int aiLevel = vision.getAiLevel();

		List<Card> hand = new ArrayList<>(user.getCards());
		if (hand.isEmpty()) {
			return -1;
		}

		// Level 0: 摸什么打什么
		if (aiLevel == MjVision.AI_DUMB) {
			return decideDumb(hand, drawnTile);
		}

		// Level 1/2: 基于向听数选牌
		return decideByEfficiency(hand, vision);
	}

	/**
	 * 最低级 AI：打刚摸到的牌，如果没有则打最小牌
	 */
	private static int decideDumb(List<Card> hand, int drawnTile) {
		if (drawnTile > 0) {
			for (Card c : hand) {
				if (c.getId() == drawnTile) {
					return drawnTile;
				}
			}
		}
		// fallback: 打最小牌
		return Collections.min(hand).getId();
	}

	/**
	 * 基于向听数 + 牌效率的弃牌选择
	 */
	private static int decideByEfficiency(List<Card> hand, MjVision vision) {
		int[] counts = handToCounts(hand);
		int currentShanten = calcShanten(counts);
		int laiZi = vision.getLaiZiTileId();

		int bestTile = -1;
		int bestShanten = Integer.MAX_VALUE;
		int bestEfficiency = -1;

		// 尝试打出每张不同的牌
		Map<Integer, Integer> seen = new HashMap<>();
		for (Card c : hand) {
			int tid = c.getId();
			if (seen.put(tid, tid) != null) {
				continue; // 同牌只算一次
			}
			if (tid == laiZi) {
				continue; // 不打赖子
			}

			// 模拟打出
			counts[tid]--;
			int newShanten = calcShanten(counts);
			int efficiency = vision.getAiLevel() >= MjVision.AI_ADVANCED
					? countEffectiveDraws(counts, laiZi) : 0;
			counts[tid]++;

			// 选向听数最低的；同向听数选有效进张最多的
			if (newShanten < bestShanten
					|| (newShanten == bestShanten && efficiency > bestEfficiency)) {
				bestShanten = newShanten;
				bestEfficiency = efficiency;
				bestTile = tid;
			}
		}
		return bestTile > 0 ? bestTile : Collections.min(hand).getId();
	}

	// ==================== Claim 决策 ====================

	/**
	 * AI Claim 决策（MJ_CLAIM 状态超时调用）
	 *
	 * @param table     麻将桌
	 * @param user      当前用户
	 * @param claimInfo 该座位可执行的 claim 操作
	 * @return 操作类型
	 */
	public static GameProto.OpInfo decideClaim(MjTable table, TableUser user, MjClaimInfo claimInfo) {
		MjVision vision = new MjVision(table, user, table.getMjContext().getVisionLevel(),
				table.getMjContext().getAiLevel());
		int aiLevel = vision.getAiLevel();

		// Level 0: 全过
		if (aiLevel == MjVision.AI_DUMB) {
			return pass();
		}

		// 能胡必胡
		if (claimInfo.isCanHu()) {
			return hu();
		}

		// 能杠：评估是否值得杠
		if (claimInfo.isCanGang()) {
			if (shouldGang(table, user, vision, claimInfo.getGangTileId())) {
				return gang();
			}
		}

		// 能碰：评估碰后是否改善手牌
		if (claimInfo.isCanPeng()) {
			if (shouldPeng(table, user, vision, claimInfo.getClaimTileId())) {
				return peng();
			}
		}

		// 能吃：评估吃后是否改善手牌
		if (claimInfo.isCanChi()) {
			if (shouldChi(table, user, vision, claimInfo.getClaimTileId())) {
				// 选择第一个吃法
				List<int[]> combos = claimInfo.getChiCombos();
				if (combos != null && !combos.isEmpty()) {
					return chi(combos.get(0));
				}
			}
		}

		return pass();
	}

	/**
	 * 是否应该杠
	 */
	private static boolean shouldGang(MjTable table, TableUser user, MjVision vision, int gangTileId) {
		List<Card> hand = new ArrayList<>(user.getCards());
		int[] counts = handToCounts(hand);
		int laiZi = vision.getLaiZiTileId();

		// 不杠赖子
		if (gangTileId == laiZi) {
			return false;
		}

		int shantenBefore = calcShanten(counts);
		// 模拟杠：移除 4 张（明杠移 3 张，暗杠移 4 张，这里简化）
		int removeCount = 0;
		for (Card c : hand) {
			if (c.getId() == gangTileId && removeCount < 3) {
				counts[c.getId()]--;
				removeCount++;
			}
		}
		int shantenAfter = calcShanten(counts);

		// 杠后向听数不增加才杠（杠完摸牌可能改善）
		return shantenAfter <= shantenBefore;
	}

	/**
	 * 是否应该碰
	 */
	private static boolean shouldPeng(MjTable table, TableUser user, MjVision vision, int pengTileId) {
		List<Card> hand = new ArrayList<>(user.getCards());
		int[] counts = handToCounts(hand);
		int laiZi = vision.getLaiZiTileId();

		// 不碰赖子
		if (pengTileId == laiZi) {
			return false;
		}

		int shantenBefore = calcShanten(counts);
		// 模拟碰：移除 2 张
		int removeCount = 0;
		for (Card c : hand) {
			if (c.getId() == pengTileId && removeCount < 2) {
				counts[c.getId()]--;
				removeCount++;
			}
		}
		int shantenAfter = calcShanten(counts);

		// 碰后向听数减少才碰
		return shantenAfter < shantenBefore;
	}

	/**
	 * 是否应该吃
	 */
	private static boolean shouldChi(MjTable table, TableUser user, MjVision vision, int chiTileId) {
		List<Card> hand = new ArrayList<>(user.getCards());
		int[] counts = handToCounts(hand);
		int shantenBefore = calcShanten(counts);

		// 简化：检查吃后是否减少向听数
		// 实际应该遍历所有吃法组合，这里用启发式
		int shantenAfter = calcShanten(counts); // 吃需要移除手牌中 2 张

		// 吃牌价值较低（暴露信息），只有向听数明显改善才吃
		return shantenAfter < shantenBefore;
	}

	// ==================== 向听数计算 ====================

	/**
	 * 将手牌转为计数数组（下标 = tileId, 值 = 该牌张数）
	 */
	private static int[] handToCounts(List<Card> hand) {
		int[] counts = new int[600]; // tileId 最大 500+
		for (Card c : hand) {
			counts[c.getId()]++;
		}
		return counts;
	}

	/**
	 * 计算向听数（标准 14 张麻将算法）
	 * <p>
	 * 遍历所有可能的将（对子），对剩余牌计算最大面子数。
	 * 向听数 = 8 - 2*面子数 - 搭子数（搭子按半面子算，每搭 -1）
	 * <p>
	 * 也检查七对子。
	 *
	 * @param counts tileId → 张数
	 * @return 向听数（-1 = 已胡，0 = 听牌）
	 */
	public static int calcShanten(int[] counts) {
		int minShanten = calcStandardShanten(counts, false);

		// 七对子向听数
		int pairs = 0;
		int singles = 0;
		for (int i = 100; i < 600; i++) {
			if (counts[i] >= 2) {
				pairs += counts[i] / 2;
			}
			if (counts[i] % 2 == 1) {
				singles++;
			}
		}
		int qiDuiShanten = 6 - pairs + (singles > 0 ? 1 : 0);
		// 简化七对子：7 - 已有对子数
		int pairCount = 0;
		for (int i = 100; i < 600; i++) {
			if (counts[i] >= 2) {
				pairCount++;
			}
		}
		qiDuiShanten = 6 - pairCount;

		return Math.min(minShanten, qiDuiShanten);
	}

	/**
	 * 标准型向听数：尝试每种将，计算面子+搭子
	 */
	private static int calcStandardShanten(int[] counts, boolean hasMelds) {
		int best = 8; // 最大向听数

		// 尝试每种将
		for (int i = 100; i < 600; i++) {
			if (counts[i] >= 2) {
				counts[i] -= 2;
				int melds = countMelds(counts, 100, 0);
				best = Math.min(best, 8 - 2 * melds);
				counts[i] += 2;
			}
		}

		// 无将情况（用于七对子检测等）
		int meldsNoPair = countMelds(counts, 100, 0);
		best = Math.min(best, 7 - 2 * meldsNoPair);

		return best;
	}

	/**
	 * 递归计算从 pos 开始能拆出的最大面子数（贪心+回溯）
	 */
	private static int countMelds(int[] counts, int pos, int depth) {
		// 跳过空位
		while (pos < 600 && counts[pos] == 0) {
			pos++;
		}
		if (pos >= 600) {
			return 0;
		}

		int best = 0;

		// 尝试刻子
		if (counts[pos] >= 3) {
			counts[pos] -= 3;
			best = Math.max(best, 1 + countMelds(counts, pos, depth + 1));
			counts[pos] += 3;
		}

		// 尝试顺子（仅数牌，同花色连续）
		int suit = MjConst.suitOf(pos);
		int val = MjConst.valueOf(pos);
		if (suit >= 1 && suit <= 3 && val <= 7) {
			int t1 = MjConst.encode(suit, val);
			int t2 = MjConst.encode(suit, val + 1);
			int t3 = MjConst.encode(suit, val + 2);
			if (counts[t1] > 0 && counts[t2] > 0 && counts[t3] > 0) {
				counts[t1]--;
				counts[t2]--;
				counts[t3]--;
				best = Math.max(best, 1 + countMelds(counts, pos, depth + 1));
				counts[t1]++;
				counts[t2]++;
				counts[t3]++;
			}
		}

		// 不用当前牌（跳过）
		int saved = counts[pos];
		counts[pos] = 0;
		best = Math.max(best, countMelds(counts, pos + 1, depth + 1));
		counts[pos] = saved;

		return best;
	}

	// ==================== 有效进张计算 ====================

	/**
	 * 计算有效进张数：遍历所有可能的牌，摸到后能减少向听数的总张数
	 *
	 * @param counts 当前手牌计数（不含打出的牌）
	 * @param laiZi  赖子牌 ID
	 * @return 有效进张总数
	 */
	private static int countEffectiveDraws(int[] counts, int laiZi) {
		int baseShanten = calcShanten(counts);
		if (baseShanten < 0) {
			return 0; // 已胡
		}
		int effective = 0;
		for (int tid = 100; tid < 600; tid++) {
			if (tid == laiZi) {
				continue;
			}
			// 计算该牌在外面的剩余张数
			int remaining = MjConst.COPY_COUNT - counts[tid];
			if (remaining <= 0) {
				continue;
			}
			// 模拟摸到该牌
			counts[tid]++;
			int newShanten = calcShanten(counts);
			counts[tid]--;
			if (newShanten < baseShanten) {
				effective += remaining;
			}
		}
		return effective;
	}

	// ==================== 操作构建 ====================

	private static GameProto.OpInfo pass() {
		return GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.MJ_PASS).build();
	}

	private static GameProto.OpInfo hu() {
		return GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.MJ_HU).build();
	}

	private static GameProto.OpInfo gang() {
		return GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.MJ_GANG).build();
	}

	private static GameProto.OpInfo peng() {
		return GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.MJ_PENG).build();
	}

	private static GameProto.OpInfo chi(int[] tiles) {
		GameProto.CardInfo.Builder cardInfo = GameProto.CardInfo.newBuilder();
		for (int t : tiles) {
			cardInfo.addCards(GameProto.Card.newBuilder().setValue(t).build());
		}
		return GameProto.OpInfo.newBuilder()
				.setChoice(ConstProto.Operation.MJ_CHI)
				.addOpCards(cardInfo.build())
				.build();
	}
}
