package game.manager.table.ddz.ai;

/**
 * DdzAiConstants
 * 简易 AI 可调参数（拆牌权重、阶段阈值、跟牌/炸弹倾向）。后续只改此处即可微调行为。
 * 
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 */
public final class DdzAiConstants {

	private DdzAiConstants() {
	}

	// ========== 阶段（按剩余手牌张数） ==========
	/** ≥ 该张数视为前期（试探，少动炸） */
	public static final int PHASE_EARLY_MIN_CARDS = 14;
	/** ≥ 该张数视为中期 */
	public static final int PHASE_MID_MIN_CARDS = 8;

	// ========== 拆牌管线分支权重（越大越倾向保留在一起，不轻易拆开首出） ==========
	public static final int SPLIT_WEIGHT_ROCKET = 5000;
	public static final int SPLIT_WEIGHT_BOMB = 3500;
	/** 飞机（带单/带对）机身按「段」加权，越大越倾向保留整副飞机 */
	public static final int SPLIT_WEIGHT_PLANE = 550;
	/** 拆出一组飞机时在 {@link #SPLIT_WEIGHT_PLANE}·k 基础上再加的固定分 */
	public static final int SPLIT_WEIGHT_PLANE_GROUP_BONUS = 900;
	public static final int SPLIT_WEIGHT_STRAIGHT_PER_CARD = 120;
	public static final int SPLIT_WEIGHT_STRAIGHT_MIN_BONUS = 400;
	public static final int SPLIT_WEIGHT_STRAIGHT_DOUBLE_PER_PAIR = 280;
	public static final int SPLIT_WEIGHT_STRAIGHT_DOUBLE_MIN_BONUS = 350;
	public static final int SPLIT_WEIGHT_TRIPLE = 900;
	public static final int SPLIT_WEIGHT_PAIR = 200;
	public static final int SPLIT_WEIGHT_SINGLE = 50;
	/** 单张 2/A/K 额外保留分 */
	public static final int SPLIT_WEIGHT_SINGLE_TOP_RANK_EXTRA = 180;

	// ========== 首出偏好（在可选合法牌型上的加减分，越小越倾向先出） ==========
	public static final int LEAD_BONUS_SINGLE_RANK_7_TO_10 = -80;
	public static final int LEAD_PENALTY_SINGLE_HIGH = 200;
	public static final int LEAD_PENALTY_PAIR_HIGH_RANK = 150;
	public static final int LEAD_BONUS_SMALL_PAIR_LOW_RANK = -60;
	public static final int LEAD_PENALTY_BOMB_EARLY = 800;
	public static final int LEAD_PENALTY_ROCKET_EARLY = 2000;
	/** 拆牌保留权重换算进首出成本：权重越高越晚出 */
	public static final double LEAD_PRESERVE_WEIGHT_SCALE = 0.35;

	// ========== 跟牌：最小压制偏好 ==========
	/** strengthKey 差值惩罚系数（略抬高大牌型成本） */
	public static final double FOLLOW_STRENGTH_MARGIN_PENALTY = 2.0;
	public static final int FOLLOW_BOMB_BASE_COST = 400;
	public static final int FOLLOW_ROCKET_COST = 2500;
	/** 前期且上一手非炸弹且点数较弱时，不使用炸弹压制的 strengthKey 上限（normalize 后） */
	public static final int FOLLOW_SOFT_LAST_STRENGTH_MAX = 8;
	public static final int FOLLOW_BOMB_WHEN_OPP_HAND_AT_MOST = 10;

	// ========== 农民配合（极简） ==========
	/** true：队友农民刚出过牌时优先 PASS，让队友跑牌 */
	public static final boolean AI_PASS_AFTER_TEAMMATE_PLAY = true;

	private static boolean isTopRankSingle(int cardVal) {
		return cardVal >= 13 && cardVal <= 15 || cardVal >= game.manager.table.card.CardConst.SMALL_JOKER_VAL;
	}

	public static int splitSingleExtra(int cardVal) {
		if (isTopRankSingle(cardVal)) {
			return SPLIT_WEIGHT_SINGLE_TOP_RANK_EXTRA;
		}
		return 0;
	}

	/** 拆牌管线里一整组飞机（带单或带对）的保留分 */
	public static int splitPlaneGroupScore(int segmentsK) {
		return SPLIT_WEIGHT_PLANE * segmentsK + SPLIT_WEIGHT_PLANE_GROUP_BONUS;
	}
}
