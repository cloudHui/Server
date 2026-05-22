package game.manager.table.card.mj;

/**
 * 麻将牌常量
 * 牌ID编码: suit*100 + value
 * 万(101-109), 条(201-209), 筒(301-309), 风(401-404东南西北), 箭(501-503中发白)
 * 每种4张，共136张
 */
public class MjConst {

	/** 万 */
	public static final int SUIT_WAN = 1;
	/** 条 */
	public static final int SUIT_TIAO = 2;
	/** 筒 */
	public static final int SUIT_TONG = 3;
	/** 风牌 东南西北 */
	public static final int SUIT_FENG = 4;
	/** 箭牌 中发白 */
	public static final int SUIT_JIAN = 5;

	/** 每种牌的副本数 */
	public static final int COPY_COUNT = 4;

	/** 数牌(万条筒)每类数量 */
	public static final int NUM_COUNT = 9;
	/** 风牌数量 */
	public static final int FENG_COUNT = 4;
	/** 箭牌数量 */
	public static final int JIAN_COUNT = 3;

	/** 总牌数 136 */
	public static final int TOTAL_TILES = (NUM_COUNT * 3 + FENG_COUNT + JIAN_COUNT) * COPY_COUNT;

	/** 每人初始手牌数 */
	public static final int INIT_HAND = 13;

	/** 牌的最小值(一万) */
	public static final int MIN_VAL = 101;
	/** 牌的最大值(白板) */
	public static final int MAX_VAL = 503;

	/**
	 * 编码牌ID
	 */
	public static int encode(int suit, int value) {
		return suit * 100 + value;
	}

	/**
	 * 获取花色
	 */
	public static int suitOf(int tileId) {
		return tileId / 100;
	}

	/**
	 * 获取牌面值(1-9或1-4或1-3)
	 */
	public static int valueOf(int tileId) {
		return tileId % 100;
	}
}
