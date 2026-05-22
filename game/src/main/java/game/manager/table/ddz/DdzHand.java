package game.manager.table.ddz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import game.manager.table.cards.Card;
import proto.ConstProto;
import proto.GameProto;

/**
 * 一手牌的解析结果，用于校验与比大小。
 * 
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 */
public class DdzHand {

	private final ConstProto.CardType type;// 牌类型
	private final List<Card> cards;// 牌
	private final boolean rocket;// 火箭
	private final boolean bomb;// 炸弹
	/** 主比较键：点数越大越强（已在规则内归一） */
	private final int strengthKey;// 主比较键
	private final int straightLen;// 顺子长度

	/**
	 * 构造函数
	 * 
	 * @param type        牌类型
	 * @param cards       牌
	 * @param rocket      火箭
	 * @param bomb        炸弹
	 * @param strengthKey 主比较键
	 * @param straightLen 顺子长度
	 */
	public DdzHand(ConstProto.CardType type, List<Card> cards, boolean rocket, boolean bomb, int strengthKey,
			int straightLen) {
		this.type = type;
		this.cards = Collections.unmodifiableList(new ArrayList<>(cards));
		this.rocket = rocket;
		this.bomb = bomb;
		this.strengthKey = strengthKey;
		this.straightLen = straightLen;
	}

	/**
	 * 获取牌类型
	 * 
	 * @return 牌类型
	 */
	public ConstProto.CardType getType() {
		return type;
	}

	/**
	 * 获取牌
	 * 
	 * @return 牌
	 */
	public List<Card> getCards() {
		return cards;
	}

	/**
	 * 是否是火箭
	 * 
	 * @return 是否是火箭
	 */
	public boolean isRocket() {
		return rocket;
	}

	/**
	 * 是否是炸弹
	 * 
	 * @return 是否是炸弹
	 */
	public boolean isBomb() {
		return bomb;
	}

	/**
	 * 获取主比较键
	 * 
	 * @return 主比较键
	 */
	public int getStrengthKey() {
		return strengthKey;
	}

	/**
	 * 获取顺子长度
	 * 
	 * @return 顺子长度
	 */
	public int getStraightLen() {
		return straightLen;
	}

	/**
	 * 转换为卡片信息
	 * 
	 * @return 卡片信息
	 */
	public GameProto.CardInfo toCardInfo() {
		GameProto.CardInfo.Builder b = GameProto.CardInfo.newBuilder().setType(type);
		for (Card c : cards) {
			b.addCards(GameProto.Card.newBuilder().setValue(c.getId()).build());
		}
		return b.build();
	}
}
