package game.manager.table.ddz.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import game.manager.table.DdzTable;
import game.manager.table.TableUser;
import game.manager.table.card.CardSuit;
import game.manager.table.cards.Card;

/**
 * 斗地主 AI 视野实现。
 * <p>
 * 根据 visionLevel 控制 AI 能看到的信息范围：
 * <ul>
 *   <li>0(NORMAL) — 自己手牌 + 已出牌</li>
 *   <li>1(SEMI)   — + 剩余牌池按 rank 分组</li>
 *   <li>2(FULL)   — + 其他玩家手牌</li>
 * </ul>
 *
 * @author cloud
 * @date 2026-06-11
 * @version 1.0
 * @since 1.0
 */
public class DdzVision implements AiVision {

	private static final List<Card> FULL_DECK = buildFullDeck();

	private final DdzTable table;
	private final TableUser self;
	private final int visionLevel;
	private final int aiLevel;
	private final int landlordSeat;

	// 缓存
	private Map<Integer, List<Card>> remainingPoolCache;

	public DdzVision(DdzTable table, TableUser self, int visionLevel, int aiLevel) {
		this.table = table;
		this.self = self;
		this.visionLevel = visionLevel;
		this.aiLevel = aiLevel;
		this.landlordSeat = table.getDdz().getLandlordSeat();
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

	@Override
	public Set<Integer> getPlayedCardIds() {
		return table.getDdz().getPlayedCardIds();
	}

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
		remainingPoolCache = new HashMap<>();
		for (Card c : FULL_DECK) {
			if (!played.contains(c.getId()) && !myIds.contains(c.getId())) {
				remainingPoolCache.computeIfAbsent(c.getCardVal(), k -> new ArrayList<>()).add(c);
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
			// 精确值：遍历对手取最小
			int min = Integer.MAX_VALUE;
			for (TableUser u : table.getUsers().values()) {
				if (u.getUserId() == self.getUserId()) {
					continue;
				}
				if (isOpponent(u)) {
					min = Math.min(min, u.getCards().size());
				}
			}
			return min == Integer.MAX_VALUE ? 20 : min;
		}
		// 非透视：用总牌数 - 已出 - 自己手牌 估算对手平均值
		int totalCards = 54;
		int played = getPlayedCardIds().size();
		int mine = self.getCards().size();
		int remaining = totalCards - played - mine;
		int oppCount = table.getUsers().size() - 1;
		return oppCount > 0 ? remaining / oppCount : remaining;
	}

	@Override
	public int remainingCountOfRank(int rank) {
		if (visionLevel < LEVEL_SEMI) {
			return -1;
		}
		Map<Integer, List<Card>> pool = getRemainingPool();
		List<Card> cards = pool.get(rank);
		return cards != null ? cards.size() : 0;
	}

	// ==================== 内部方法 ====================

	/**
	 * 判断某用户是否是自己的对手
	 */
	private boolean isOpponent(TableUser u) {
		if (landlordSeat < 0) {
			return true; // 未定地主时，都算对手
		}
		boolean selfIsLandlord = self.getSeated() == landlordSeat;
		boolean otherIsLandlord = u.getSeated() == landlordSeat;
		return selfIsLandlord != otherIsLandlord; // 不同阵营 = 对手
	}

	/**
	 * 构建完整 54 张牌库
	 */
	private static List<Card> buildFullDeck() {
		List<Card> deck = new ArrayList<>(54);
		for (CardSuit suit : CardSuit.values()) {
			for (int id = suit.getStartVal(); id <= suit.getEndVal(); id++) {
				deck.add(new Card(id));
			}
		}
		return Collections.unmodifiableList(deck);
	}
}
