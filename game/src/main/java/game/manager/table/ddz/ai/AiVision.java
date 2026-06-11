package game.manager.table.ddz.ai;

import java.util.List;
import java.util.Map;
import java.util.Set;

import game.manager.table.cards.Card;

/**
 * AI 视野抽象接口。
 * <p>
 * 控制 AI 能看到多少信息，支持三种级别：
 * <ul>
 *   <li>LEVEL_NORMAL(0) — 只看自己手牌 + 已出牌</li>
 *   <li>LEVEL_SEMI(1) — 额外知道剩余牌池构成</li>
 *   <li>LEVEL_FULL(2) — 额外知道其他玩家手牌</li>
 * </ul>
 * DDZ 和麻将各自实现此接口，AI 决策只通过此接口获取信息。
 *
 * @author cloud
 * @date 2026-06-11
 * @version 1.0
 * @since 1.0
 */
public interface AiVision {

	// ========== 视野等级（visionLevel） ==========
	/** 正常模式：只看自己 */
	int LEVEL_NORMAL = 0;
	/** 半透视：自己 + 剩余牌池 */
	int LEVEL_SEMI = 1;
	/** 全透视：自己 + 剩余牌池 + 他人手牌 */
	int LEVEL_FULL = 2;

	// ========== AI 智能等级（aiLevel） ==========
	/** 最低级：无决策，过/出最小牌/摸什么打什么 */
	int AI_DUMB = 0;
	/** 基础策略：简单启发式出牌/配合 */
	int AI_BASIC = 1;
	/** 高级策略：向听数/拆牌/概率推算 */
	int AI_ADVANCED = 2;

	/** 当前视野等级 */
	int getVisionLevel();

	/** 当前 AI 智能等级 */
	int getAiLevel();

	/** 自己的手牌（始终可用） */
	List<Card> getMyHand();

	/** 已出牌 ID 集合（记牌器） */
	Set<Integer> getPlayedCardIds();

	/**
	 * 剩余未出牌按 rank 分组（level ≥ 1 时可用，否则返回 null）。
	 * key = cardVal, value = 该 rank 剩余的 Card 列表
	 */
	Map<Integer, List<Card>> getRemainingPool();

	/**
	 * 指定座位的手牌（level = 2 时可用，否则返回 null）。
	 */
	List<Card> getOpponentHand(int seat);

	/**
	 * 对手最少手牌张数。
	 * level = 2 时精确，否则根据己方手牌 + 已出牌估算。
	 */
	int getMinOpponentCards();

	/**
	 * 指定 rank 在外面（非自己手牌、非已出）还剩几张。
	 * level ≥ 1 时精确，否则返回 -1 表示未知。
	 */
	int remainingCountOfRank(int rank);
}
