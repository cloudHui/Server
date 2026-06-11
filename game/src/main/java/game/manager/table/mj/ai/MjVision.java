package game.manager.table.mj.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import game.manager.table.MjTable;
import game.manager.table.TableUser;
import game.manager.table.card.mj.MjConst;
import game.manager.table.cards.Card;
import game.manager.table.ddz.ai.AiVision;
import game.manager.table.mj.MjExposedSet;

/**
 * 麻将 AI 视野实现。
 * <p>
 * 根据 visionLevel 控制 AI 能看到的信息范围：
 * <ul>
 *   <li>0(NORMAL) — 自己手牌 + 各家弃牌</li>
 *   <li>1(SEMI)   — + 牌山剩余牌构成</li>
 *   <li>2(FULL)   — + 其他玩家手牌</li>
 * </ul>
 *
 * @author cloud
 * @date 2026-06-11
 * @version 1.0
 * @since 1.0
 */
public class MjVision implements AiVision {

	private final MjTable table;
	private final TableUser self;
	private final int visionLevel;
	private final int aiLevel;

	// 缓存
	private Map<Integer, List<Card>> remainingPoolCache;
	private Set<Integer> playedCardIdsCache;

	public MjVision(MjTable table, TableUser self, int visionLevel, int aiLevel) {
		this.table = table;
		this.self = self;
		this.visionLevel = visionLevel;
		this.aiLevel = aiLevel;
	}

	@Override
	public int getVisionLevel() {
		return visionLevel;
	}

	@Override
	public int getAiLevel() {
		return aiLevel;
	}

	@Override
	public List<Card> getMyHand() {
		return self.getCards();
	}

	/**
	 * 获取所有已打出的牌 ID 集合（各家弃牌汇总）
	 */
	@Override
	public Set<Integer> getPlayedCardIds() {
		if (playedCardIdsCache != null) {
			return playedCardIdsCache;
		}
		playedCardIdsCache = new HashSet<>();
		int seatNum = table.getTableModel().getSeatNum();
		for (int seat = 0; seat < seatNum; seat++) {
			List<Integer> pile = table.getMjContext().getDiscardPile(seat);
			if (pile != null) {
				playedCardIdsCache.addAll(pile);
			}
		}
		return playedCardIdsCache;
	}

	/**
	 * 剩余未出牌按 value 分组（level ≥ 1 时可用）。
	 * key = tileId, value = 该牌剩余的 Card 列表
	 */
	@Override
	public Map<Integer, List<Card>> getRemainingPool() {
		if (visionLevel < LEVEL_SEMI) {
			return null;
		}
		if (remainingPoolCache != null) {
			return remainingPoolCache;
		}
		Set<Integer> played = getPlayedCardIds();
		Set<Integer> myIds = new HashSet<>();
		for (Card c : self.getCards()) {
			myIds.add(c.getId());
		}
		// 也扣除副露区的牌
		Set<Integer> exposedIds = new HashSet<>();
		int seatNum = table.getTableModel().getSeatNum();
		for (int seat = 0; seat < seatNum; seat++) {
			List<MjExposedSet> sets = table.getMjContext().getExposedSets(seat);
			if (sets != null) {
				for (MjExposedSet es : sets) {
					for (int tid : es.getTileIds()) {
						exposedIds.add(tid);
					}
				}
			}
		}

		remainingPoolCache = new HashMap<>();
		// 构建完整牌库 136 张
		for (int suit = 1; suit <= 5; suit++) {
			int maxVal = suit <= 3 ? 9 : (suit == 4 ? 4 : 3);
			for (int val = 1; val <= maxVal; val++) {
				int baseTileId = suit * 100 + val;
				for (int copy = 0; copy < MjConst.COPY_COUNT; copy++) {
					int tileId = baseTileId; // 同值同 ID
					if (!played.contains(tileId) && !myIds.contains(tileId) && !exposedIds.contains(tileId)) {
						remainingPoolCache.computeIfAbsent(tileId, k -> new ArrayList<>()).add(new Card(tileId));
					}
				}
			}
		}
		return remainingPoolCache;
	}

	@Override
	public List<Card> getOpponentHand(int seat) {
		if (visionLevel < LEVEL_FULL) {
			return null;
		}
		TableUser user = table.getSeatUser(seat);
		if (user == null || user.getUserId() == self.getUserId()) {
			return null;
		}
		return user.getCards();
	}

	@Override
	public int getMinOpponentCards() {
		if (visionLevel >= LEVEL_FULL) {
			int min = Integer.MAX_VALUE;
			for (TableUser u : table.getUsers().values()) {
				if (u.getUserId() != self.getUserId()) {
					min = Math.min(min, u.getCards().size());
				}
			}
			return min == Integer.MAX_VALUE ? 13 : min;
		}
		// 非透视：返回平均值估算
		int totalRemaining = 136 - getPlayedCardIds().size() - self.getCards().size()
				- countExposedTiles();
		int oppCount = table.getUsers().size() - 1;
		return oppCount > 0 ? totalRemaining / oppCount : 13;
	}

	@Override
	public int remainingCountOfRank(int tileId) {
		if (visionLevel < LEVEL_SEMI) {
			return -1;
		}
		Map<Integer, List<Card>> pool = getRemainingPool();
		List<Card> cards = pool.get(tileId);
		return cards != null ? cards.size() : 0;
	}

	// ==================== 麻将专用方法 ====================

	/**
	 * 获取指定座位的副露列表
	 */
	public List<MjExposedSet> getExposedSets(int seat) {
		List<MjExposedSet> sets = table.getMjContext().getExposedSets(seat);
		return sets != null ? sets : java.util.Collections.emptyList();
	}

	/**
	 * 获取赖子牌 ID（0 表示无赖子）
	 */
	public int getLaiZiTileId() {
		return table.getMjContext().getLaiZiTileId();
	}

	/**
	 * 获取指定座位的弃牌列表
	 */
	public List<Integer> getDiscardPile(int seat) {
		List<Integer> pile = table.getMjContext().getDiscardPile(seat);
		return pile != null ? pile : java.util.Collections.emptyList();
	}

	// ==================== 内部方法 ====================

	private int countExposedTiles() {
		int count = 0;
		int seatNum = table.getTableModel().getSeatNum();
		for (int seat = 0; seat < seatNum; seat++) {
			List<MjExposedSet> sets = table.getMjContext().getExposedSets(seat);
			if (sets != null) {
				for (MjExposedSet es : sets) {
					count += es.getTileIds().size();
				}
			}
		}
		return count;
	}
}
