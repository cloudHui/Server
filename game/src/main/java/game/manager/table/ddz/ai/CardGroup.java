package game.manager.table.ddz.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import game.manager.table.cards.Card;

/**
 * 拆牌后的一个出牌单元（不一定已是合法统一牌型，需经 {@link game.manager.table.ddz.DdzRules#analyze}）。
 */
public final class CardGroup {

	private final List<Card> cards;
	private final int preserveScore;

	public CardGroup(List<Card> cards, int preserveScore) {
		this.cards = Collections.unmodifiableList(new ArrayList<>(cards));
		this.preserveScore = preserveScore;
	}

	public List<Card> getCards() {
		return cards;
	}

	public int getPreserveScore() {
		return preserveScore;
	}
}
