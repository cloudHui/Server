package web.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.Message;
import msg.registor.message.GMsg;
import proto.GameProto;

/**
 * 将 Gate 推送的 Protobuf 转为前端 WebSocket JSON，独立于连接管理，便于复用与单测。
 */
final class GameWsPushFormatter {

	private GameWsPushFormatter() {}

	static String pushAction(int msgId) {
		if (msgId == GMsg.NOT_CARD) return "notCard";
		if (msgId == GMsg.NOT_OP) return "notOp";
		if (msgId == GMsg.ACK_OP) return "ackOp";
		if (msgId == GMsg.NOT_STATE || msgId == GMsg.NOT_TABLE_STATE) return "notState";
		if (msgId == GMsg.NOT_RESULT) return "notResult";
		if (msgId == GMsg.MJ_TILE_NOT) return "notMjState";
		if (msgId == GMsg.NOT_ROUND_RESULT) return "notRoundResult";
		if (msgId == GMsg.NOT_GAME_RESULT) return "notGameResult";
		if (msgId == GMsg.ACK_ENTER_TABLE_MSG) return "seatUpdate";
		return null;
	}

	static Object formatPush(int msgId, Message proto) {
		if (proto instanceof GameProto.AckEnterTable) {
			GameProto.AckEnterTable ack = (GameProto.AckEnterTable) proto;
			Map<String, Object> m = new HashMap<>();
			m.put("players", formatPlayers(ack.getPlayersList(), 0));
			if (ack.hasTableInfo()) {
				m.put("tableInfo", formatTableInfo(ack.getTableInfo()));
			}
			return m;
		}
		if (proto instanceof GameProto.NotCard) {
			return formatNotCard((GameProto.NotCard) proto);
		}
		if (proto instanceof GameProto.NotOperation) {
			return formatNotOp((GameProto.NotOperation) proto);
		}
		if (proto instanceof GameProto.AckOp) {
			return formatAckOp((GameProto.AckOp) proto);
		}
		if (proto instanceof GameProto.NotTableState) {
			Map<String, Object> m = new HashMap<>();
			m.put("state", ((GameProto.NotTableState) proto).getState());
			return m;
		}
		if (proto instanceof GameProto.NotResult) {
			return formatNotResult((GameProto.NotResult) proto);
		}
		if (proto instanceof GameProto.NotMjState) {
			return formatNotMjState((GameProto.NotMjState) proto);
		}
		if (proto instanceof GameProto.NotRoundResult) {
			return formatNotRoundResult((GameProto.NotRoundResult) proto);
		}
		if (proto instanceof GameProto.NotGameResult) {
			return formatNotGameResult((GameProto.NotGameResult) proto);
		}
		return new HashMap<>();
	}

	static List<Map<String, Object>> formatPlayers(List<GameProto.Player> players, int currentRoleId) {
		List<Map<String, Object>> result = new ArrayList<>();
		for (GameProto.Player player : players) {
			Map<String, Object> p = new HashMap<>();
			p.put("roleId", player.getRoleId());
			p.put("position", player.getPosition());
			p.put("nickName", player.getNickName().toStringUtf8());
			p.put("cardCount", player.getCardsCount());
			p.put("robot", player.getRoleId() < 0);
			if (currentRoleId != 0 && player.getRoleId() == currentRoleId && player.getCardsCount() > 0) {
				List<Integer> cardValues = new ArrayList<>();
				for (GameProto.Card card : player.getCardsList()) {
					cardValues.add(card.getValue());
				}
				p.put("cards", cardValues);
			}
			result.add(p);
		}
		return result;
	}

	static Map<String, Object> formatTableInfo(GameProto.TableInfo tableInfo) {
		Map<String, Object> result = new HashMap<>();
		result.put("roomId", tableInfo.getRoomId());
		result.put("tableId", tableInfo.getTableId());
		result.put("landlord", tableInfo.getLandlord());
		return result;
	}

	private static Map<String, Object> formatAckOp(GameProto.AckOp ack) {
		Map<String, Object> m = new HashMap<>();
		m.put("opId", ack.getOpId());
		m.put("opFrom", ack.getOpFrom());
		if (ack.hasOp()) {
			m.put("choice", ack.getOp().getChoiceValue());
			List<Integer> cards = new ArrayList<>();
			for (GameProto.CardInfo cardInfo : ack.getOp().getOpCardsList()) {
				for (GameProto.Card card : cardInfo.getCardsList()) {
					cards.add(card.getValue());
				}
			}
			m.put("cards", cards);
		}
		return m;
	}

	private static Map<String, Object> formatNotCard(GameProto.NotCard n) {
		Map<String, Object> m = new HashMap<>();
		List<Map<String, Object>> nCards = new ArrayList<>();
		for (GameProto.NCardsInfo info : n.getNCardsList()) {
			Map<String, Object> c = new HashMap<>();
			c.put("roleId", info.getRoleId());
			List<Map<String, Object>> cards = new ArrayList<>();
			for (GameProto.Card card : info.getCardsList()) {
				Map<String, Object> cv = new HashMap<>();
				cv.put("value", card.getValue());
				cards.add(cv);
			}
			c.put("cards", cards);
			nCards.add(c);
		}
		m.put("nCards", nCards);
		return m;
	}

	private static Map<String, Object> formatNotOp(GameProto.NotOperation n) {
		Map<String, Object> m = new HashMap<>();
		m.put("opSeat", n.getOpSeat());
		m.put("wait", n.getWait());
		m.put("choice", formatOpChoices(n.getChoiceList()));
		return m;
	}

	private static Map<String, Object> formatNotResult(GameProto.NotResult n) {
		Map<String, Object> m = new HashMap<>();
		m.put("winner", n.getWinner());
		m.put("landlord_id", n.getLandlordId());
		m.put("win_team", n.getWinTeam());
		m.put("base_score", n.getBaseScore());
		m.put("rob_multiplier", n.getRobMultiplier());
		m.put("spring", n.getSpring());
		m.put("anti_spring", n.getAntiSpring());
		m.put("settle_factor", n.getSettleFactor());
		List<Map<String, Object>> players = new ArrayList<>();
		for (GameProto.RPlayer p : n.getRPlayersList()) {
			Map<String, Object> rp = new HashMap<>();
			rp.put("roleId", p.getRoleId());
			List<Integer> cards = new ArrayList<>();
			for (GameProto.Card c : p.getCardsList()) {
				cards.add(c.getValue());
			}
			rp.put("cards", cards);
			players.add(rp);
		}
		m.put("rPlayers", players);
		return m;
	}

	private static Map<String, Object> formatNotMjState(GameProto.NotMjState n) {
		Map<String, Object> m = new HashMap<>();
		m.put("opSeat", n.getOpSeat());
		m.put("tileId", n.getTileId());
		m.put("action", n.getActionValue());
		m.put("wait", n.getWait());
		m.put("wallLeft", n.getWallLeft());
		m.put("choice", formatOpChoices(n.getChoiceList()));
		return m;
	}

	private static List<Map<String, Object>> formatOpChoices(List<GameProto.OpInfo> ops) {
		List<Map<String, Object>> choices = new ArrayList<>();
		for (GameProto.OpInfo op : ops) {
			Map<String, Object> c = new HashMap<>();
			c.put("choice", op.getChoiceValue());
			List<Map<String, Object>> cards = new ArrayList<>();
			for (GameProto.CardInfo cardInfo : op.getOpCardsList()) {
				for (GameProto.Card card : cardInfo.getCardsList()) {
					Map<String, Object> cv = new HashMap<>();
					cv.put("value", card.getValue());
					cards.add(cv);
				}
			}
			if (!cards.isEmpty()) {
				c.put("cards", cards);
			}
			choices.add(c);
		}
		return choices;
	}

	private static Map<String, Object> formatNotRoundResult(GameProto.NotRoundResult n) {
		Map<String, Object> m = new HashMap<>();
		m.put("round", n.getRound());
		m.put("winnerSeat", n.getWinnerSeat());
		m.put("fan", n.getFan());
		m.put("winType", n.getWinType().toStringUtf8());
		m.put("winTile", n.getWinTile());
		List<Map<String, Object>> scores = new ArrayList<>();
		for (GameProto.SeatScore s : n.getSeatScoresList()) {
			Map<String, Object> sc = new HashMap<>();
			sc.put("seat", s.getSeat());
			sc.put("score", s.getScore());
			scores.add(sc);
		}
		m.put("seatScores", scores);
		List<Map<String, Object>> hands = new ArrayList<>();
		for (GameProto.HandInfo h : n.getHandsList()) {
			Map<String, Object> hi = new HashMap<>();
			hi.put("seat", h.getSeat());
			hi.put("handTiles", h.getHandTilesList());
			hands.add(hi);
		}
		m.put("hands", hands);
		return m;
	}

	private static Map<String, Object> formatNotGameResult(GameProto.NotGameResult n) {
		Map<String, Object> m = new HashMap<>();
		m.put("totalRounds", n.getTotalRounds());
		m.put("completedRounds", n.getCompletedRounds());
		List<Map<String, Object>> totals = new ArrayList<>();
		for (GameProto.SeatScore s : n.getTotalScoresList()) {
			Map<String, Object> sc = new HashMap<>();
			sc.put("seat", s.getSeat());
			sc.put("score", s.getScore());
			totals.add(sc);
		}
		m.put("totalScores", totals);
		List<Map<String, Object>> rounds = new ArrayList<>();
		for (GameProto.RoundSummary r : n.getRoundsList()) {
			Map<String, Object> rs = new HashMap<>();
			rs.put("round", r.getRound());
			rs.put("winnerSeat", r.getWinnerSeat());
			rs.put("fan", r.getFan());
			rs.put("winType", r.getWinType().toStringUtf8());
			rounds.add(rs);
		}
		m.put("rounds", rounds);
		return m;
	}
}
