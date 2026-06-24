package game.manager.table.cards;


import game.manager.table.card.CardConst;
import game.manager.table.card.CardSuit;

/**
 * 扑克牌/麻将牌基础模型
 * 通过id编码花色和点数，支持比较、哈希、大小判断
 */
public class Card implements Comparable<Card> {

	private int id;
	private boolean isBottom;

	public Card(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	/** 获取牌面点数（id % 100） */
	public int getCardVal() {
		return this.getId() % 100;
	}

	public void setId(int id) {
		this.id = id;
	}

	/** 获取牌的花色 */
	public CardSuit getCardSuit() {
		return CardSuit.get(this.getId() / 100);
	}

	@Override
	public String toString() {
		return "Card{" + "id=" + id + '}';
	}

	/** 是否小王 */
	public boolean isSmallJoker() {
		return this.getCardVal() == CardConst.SMALL_JOKER_VAL;
	}

	/** 是否大王 */
	public boolean isBigJoker() {
		return this.getCardVal() == CardConst.BIG_JOKER_VAL;
	}

	public boolean isBottom() {
		return isBottom;
	}

	public void setBottom(boolean bottom) {
		isBottom = bottom;
	}

	/** 按点数降序比较，点数相同按id降序 */
	@Override
	public int compareTo(Card o) {
		if (this.getCardVal() == o.getCardVal()) {
			return o.id - this.id;
		}
		return o.getCardVal() - this.getCardVal();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Card)) return false;
		return this.id == ((Card) obj).id;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(id);
	}
}
