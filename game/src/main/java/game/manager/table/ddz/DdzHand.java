package game.manager.table.ddz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import game.manager.table.cards.Card;
import proto.ConstProto;
import proto.GameProto;

/**
 * 斗地主一手牌的解析结果
 * 包含牌型、强度、是否炸弹/火箭，用于校验与比大小
 */
public class DdzHand {

	private final ConstProto.CardType type;
	private final List<Card> cards;
	private final boolean rocket;
	private final boolean bomb;
	private final int strengthKey;
	private final int straightLen;

	public DdzHand(ConstProto.CardType type, List<Card> cards, boolean rocket, boolean bomb,
			int strengthKey, int straightLen) {
		this.type = type;
		this.cards = Collections.unmodifiableList(new ArrayList<>(cards));
		this.rocket = rocket;
		this.bomb = bomb;
		this.strengthKey = strengthKey;
		this.straightLen = straightLen;
	}

	public ConstProto.CardType getType() { return type; }
	public List<Card> getCards() { return cards; }
	/** 是否火箭（大小王组合） */
	public boolean isRocket() { return rocket; }
	/** 是否炸弹（四张同点） */
	public boolean isBomb() { return bomb; }
	/** 主比较键：点数越大越强 */
	public int getStrengthKey() { return strengthKey; }
	/** 顺子长度（非顺子为0） */
	public int getStraightLen() { return straightLen; }

	/** 转换为协议 CardInfo 消息 */
	public GameProto.CardInfo toCardInfo() {
		GameProto.CardInfo.Builder b = GameProto.CardInfo.newBuilder().setType(type);
		for (Card c : cards) {
			b.addCards(GameProto.Card.newBuilder().setValue(c.getId()).build());
		}
		return b.build();
	}
}
