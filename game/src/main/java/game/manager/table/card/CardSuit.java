package game.manager.table.card;

import java.util.HashMap;
import java.util.Map;

/**
 * 扑克牌花色
 */
public enum CardSuit {

	D(1, "方块", 103, 115),
	C(2, "梅花", 203, 215),
	H(3, "红桃", 303, 315),
	S(4, "黑桃", 403, 415),
	J(5, "JOKER", 516, 517),
	;

	private final int id;
	private final String intro;
	/**
	 * 牌面值的起始值 3:3
	 */
	private final int startVal;
	/**
	 * 牌面值的结束值 11:J;12:Q;13:K;14:A;15:2;516:小王;517:大王
	 */
	private final int endVal;

	private static final Map<Integer, CardSuit> es = new HashMap<>();

	static {
		for (CardSuit e : values()) {
			es.put(e.getId(), e);
		}
	}

	CardSuit(final int id, final String intro, final int startVal, final int endVal) {
		this.id = id;
		this.intro = intro;
		this.startVal = startVal;
		this.endVal = endVal;
	}

	public static CardSuit get(int id) {
		return es.get(id);
	}

	public int getId() {
		return this.id;
	}

	public String getIntro() {
		return this.intro;
	}

	public int getStartVal() {
		return startVal;
	}

	public int getEndVal() {
		return endVal;
	}

	public static Map<Integer, CardSuit> getEs() {
		return es;
	}
}