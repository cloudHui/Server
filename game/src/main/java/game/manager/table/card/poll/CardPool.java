package game.manager.table.card.poll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.card.CardSuit;
import game.manager.table.cards.Card;
import msg.registor.message.GMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GameProto;

/**
 * 牌池
 */
public class CardPool {

	private static final Logger logger = LoggerFactory.getLogger(CardPool.class);

	private final List<Card> poolCards;

	private final Table table;

	public CardPool(Table table) {
		poolCards = new ArrayList<>();
		this.table = table;
	}

	/**
	 * 初始化牌数据
	 */
	public void initCards() {
		poolCards.clear();
		for (Map.Entry<Integer, CardSuit> entry : CardSuit.getEs().entrySet()) {
			CardSuit suit = entry.getValue();
			for (int cardVal = suit.getStartVal(); cardVal <= suit.getEndVal(); cardVal++) {
				poolCards.add(new Card(cardVal));
			}
		}
		Collections.shuffle(poolCards);
	}

	public Card dealCard() {
		return poolCards.remove(leftSize() - 1);
	}

	public int leftSize() {
		return poolCards.size();
	}

	/**
	 * 发初始牌
	 */
	public void dealInitCard() {
		Map<Integer, TableUser> seatUsers = table.getSeatUsers();
		for (int index = 0; index < table.getTableModel().getCardNum(); index++) {
			for (Map.Entry<Integer, TableUser> entry : seatUsers.entrySet()) {
				entry.getValue().addCards(dealCard());
			}
		}
		//发牌通知
		sendInitCardNotice(seatUsers);
	}

	/**
	 * 发牌通知
	 */
	private void sendInitCardNotice(Map<Integer, TableUser> seatUsers) {
		GameProto.NotCard.Builder builder;
		TableUser sendUser;
		TableUser otherUser;
		boolean owner;
		GameProto.NCardsInfo.Builder nCards;
		for (Map.Entry<Integer, TableUser> entry : seatUsers.entrySet()) {
			sendUser = entry.getValue();
			builder = GameProto.NotCard.newBuilder();
			for (Map.Entry<Integer, TableUser> userEntry : seatUsers.entrySet()) {
				otherUser = userEntry.getValue();
				nCards = GameProto.NCardsInfo.newBuilder().setRoleId(otherUser.getUserId());
				owner = otherUser.equals(sendUser);
				for (Card card : otherUser.getCards()) {
					nCards.addCards(GameProto.Card.newBuilder()
							.setValue(owner ? card.getId() : 0)
							.build());
				}
				builder.addNCards(nCards.build());
			}
			logger.info("table:{} role:{} sendCard:{}", table.getTableId(), sendUser.getCards(), builder.toString());
			sendUser.sendRoleMessage(builder.build(), GMsg.NOT_CARD, table.getTableId());
		}
	}
}
