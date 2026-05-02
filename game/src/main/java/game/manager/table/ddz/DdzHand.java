package game.manager.table.ddz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import game.manager.table.cards.Card;
import proto.ConstProto;
import proto.GameProto;

/**
 * 一手牌的解析结果，用于校验与比大小。
 */
public class DdzHand {

	private final ConstProto.CardType type;
	private final List<Card> cards;
	private final boolean rocket;
	private final boolean bomb;
	/** 主比较键：点数越大越强（已在规则内归一） */
	private final int strengthKey;
	private final int straightLen;

	public DdzHand(ConstProto.CardType type, List<Card> cards, boolean rocket, boolean bomb, int strengthKey, int straightLen) {
		this.type = type;
		this.cards = Collections.unmodifiableList(new ArrayList<>(cards));
		this.rocket = rocket;
		this.bomb = bomb;
		this.strengthKey = strengthKey;
		this.straightLen = straightLen;
	}

	public ConstProto.CardType getType() {
		return type;
	}

	public List<Card> getCards() {
		return cards;
	}

	public boolean isRocket() {
		return rocket;
	}

	public boolean isBomb() {
		return bomb;
	}

	public int getStrengthKey() {
		return strengthKey;
	}

	public int getStraightLen() {
		return straightLen;
	}

	public GameProto.CardInfo toCardInfo() {
		GameProto.CardInfo.Builder b = GameProto.CardInfo.newBuilder().setType(type);
		for (Card c : cards) {
			b.addCards(GameProto.Card.newBuilder().setValue(c.getId()).build());
		}
		return b.build();
	}
}
