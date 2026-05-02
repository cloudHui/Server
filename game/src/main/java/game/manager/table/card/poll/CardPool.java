package game.manager.table.card.poll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.cards.Card;
import game.manager.table.card.CardSuit;
import msg.registor.message.GMsg;
import proto.GameProto;

/**
 * 斗地主牌池：54 张、三家各 17 张、底牌 3 张。
 */
public class CardPool {

	private static final Logger logger = LoggerFactory.getLogger(CardPool.class);

	private final List<Card> poolCards = new ArrayList<>();
	private final List<Card> bottomCards = new ArrayList<>();
	private final Table table;

	public CardPool(Table table) {
		this.table = table;
	}

	public List<Card> getBottomCards() {
		return bottomCards;
	}

	/**
	 * 初始化并洗牌
	 */
	public void initCards() {
		poolCards.clear();
		bottomCards.clear();
		for (Map.Entry<Integer, CardSuit> entry : CardSuit.getEs().entrySet()) {
			CardSuit suit = entry.getValue();
			for (int cardId = suit.getStartVal(); cardId <= suit.getEndVal(); cardId++) {
				poolCards.add(new Card(cardId));
			}
		}
		Collections.shuffle(poolCards);
	}

	public Card dealCard() {
		return poolCards.remove(poolCards.size() - 1);
	}

	public int leftSize() {
		return poolCards.size();
	}

	/**
	 * 发初始手牌与底牌，并推送 {@link GMsg#NOT_CARD}。
	 */
	public void dealInitCard() {
		initCards();
		Map<Integer, TableUser> seatUsers = table.getSeatUsers();
		int seatNum = table.getTableModel().getSeatNum();
		int perPlayer = 17;
		int bottom = 3;
		TreeMap<Integer, TableUser> ordered = new TreeMap<>(seatUsers);
		for (int round = 0; round < perPlayer; round++) {
			for (int s = 0; s < seatNum; s++) {
				TableUser u = ordered.get(s);
				if (u != null) {
					u.addCards(dealCard());
				}
			}
		}
		bottomCards.clear();
		for (int i = 0; i < bottom; i++) {
			bottomCards.add(dealCard());
		}
		sendInitCardNotice(seatUsers);
	}

	/**
	 * 将底牌并入地主手牌并再次通知手牌。
	 */
	public void attachBottomToLandlord(Table table, int landlordSeat) {
		TableUser landlord = table.getSeatUser(landlordSeat);
		if (landlord == null) {
			logger.error("attachBottomToLandlord landlord null seat:{} table:{}", landlordSeat, table.getTableId());
			return;
		}
		for (Card c : bottomCards) {
			landlord.addCards(c);
		}
		bottomCards.clear();
		sendInitCardNotice(table.getSeatUsers());
	}

	private void sendInitCardNotice(Map<Integer, TableUser> seatUsers) {
		for (Map.Entry<Integer, TableUser> entry : seatUsers.entrySet()) {
			TableUser sendUser = entry.getValue();
			GameProto.NotCard.Builder builder = GameProto.NotCard.newBuilder();
			for (Map.Entry<Integer, TableUser> userEntry : seatUsers.entrySet()) {
				TableUser otherUser = userEntry.getValue();
				GameProto.NCardsInfo.Builder nCards = GameProto.NCardsInfo.newBuilder().setRoleId(otherUser.getUserId());
				boolean owner = otherUser.equals(sendUser);
				for (Card card : otherUser.getCards()) {
					nCards.addCards(GameProto.Card.newBuilder().setValue(owner ? card.getId() : 0).build());
				}
				builder.addNCards(nCards.build());
			}
			sendUser.sendRoleMessage(builder.build(), GMsg.NOT_CARD, table.getTableId());
			logger.info("table:{} role:{} sendCardNotify", table.getTableId(), sendUser.getUserId());
		}
	}
}
