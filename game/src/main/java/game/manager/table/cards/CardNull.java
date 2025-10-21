package game.manager.table.cards;


import game.manager.table.card.CardConst;

/**
 * 空牌
 */
public class CardNull extends Card {

	public CardNull() {
		super(CardConst.NULL_VAL);
	}

	public void setId(int id) {
		super.setId(CardConst.NULL_VAL);
	}
}
