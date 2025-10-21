package game.manager.table.cards;


import game.manager.table.card.CardConst;
import game.manager.table.card.CardSuit;

public class Card implements Comparable<Card> {

	/**
	 * 牌的唯一值
	 */
	private int id;

	/**
	 * 是否为底牌
	 */
	private boolean isBottom;

	public Card(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public int getCardVal() {
		return this.getId() % 100;
	}

	public void setId(int id) {
		this.id = id;
	}

	/**
	 * 获得牌的花色
	 */
	public CardSuit getCardSuit() {
		return CardSuit.get(this.getId() / 100);
	}

	@Override
	public String toString() {
		return "Card{" + "id=" + id + '}';
	}

	public int getScore() {
		return this.getCardVal();
	}

	public boolean isSmallJoker() {
		return this.getCardVal() == CardConst.SMALL_JOKER_VAL;
	}

	public boolean isBigJoker() {
		return this.getCardVal() == CardConst.BIG_JOKER_VAL;
	}

	public boolean isBottom() {
		return isBottom;
	}

	public void setBottom(boolean bottom) {
		isBottom = bottom;
	}

	@Override
	public int compareTo(Card o) {
		if (this.getCardVal() == o.getCardVal()) {
			return o.id - this.id;
		} else {
			return o.getCardVal() - this.getCardVal();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Card) {
			Card card = (Card) obj;
			if (this.getId() == card.getId()) {
				return true;
			}
		}
		return super.equals(obj);
	}
}
