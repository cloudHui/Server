package game.manager.table.mj;

import com.google.protobuf.ByteString;
import game.manager.table.GameResult;
import game.manager.table.MjTable;
import game.manager.table.TableUser;
import game.manager.table.cards.Card;
import game.manager.table.replay.MjReplayRecorder;
import game.db.ScoreRepository;
import model.tablemodel.TableModel;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import proto.ConstProto;
import proto.GameProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 麻将结算服务
 * 处理流局、胡牌结算、单局/总结算通知、副露同步、广播
 */
public class MjSettleService {

	private static final Logger logger = LoggerFactory.getLogger(MjSettleService.class);

	private MjSettleService() {}

	// ======================== 流局结算 ========================

	/** 结束本局(流局) */
	public static void finishGame(MjTable table, String reason) {
		logger.info("麻将局结束, table: {}, round: {}, reason: {}", table.getTableId(), table.getCurrentRound(), reason);

		int seatNum = table.getTableModel().getSeatNum();
		int[] scores = new int[seatNum];

		MjReplayRecorder replay = (MjReplayRecorder) table.getReplayRecorder();
		if (replay != null) {
			replay.recordDrawGame();
			writeFinalState(table, replay);
			replay.writeSettlement(-1, 0, "liuJu", scores);
			replay.save();
		}

		table.getGameResult().addRound(table.getCurrentRound(), -1, 0, scores, "liuJu");
		ScoreRepository.getInstance().saveRound(table);
		sendRoundResult(table, -1, 0, scores, "liuJu");
		table.upNextState(TableState.TABLE_OVER);
	}

	// ======================== 胡牌结算 ========================

	/** 胡牌结算（包内可见，供MjDrawService和MjClaimService调用） */
	static void finishGameWithWin(MjTable table, MjWinResult winResult) {
		int seatNum = table.getTableModel().getSeatNum();
		MjScoring scoring = MjPlayService.createScoring(table);
		MjTableContext ctx = table.getMjContext();
		TableModel model = table.getTableModel();

		int fan = scoring.calcFan(winResult, ctx, model);
		int[] scores = scoring.settle(winResult, fan, ctx, model, seatNum);

		String winType = determineWinType(winResult);

		MjReplayRecorder replay = (MjReplayRecorder) table.getReplayRecorder();
		if (replay != null) {
			writeFinalState(table, replay);
			replay.writeSettlement(winResult.getWinnerId(), fan, winType, scores);
			replay.save();
		}

		table.getGameResult().addRound(table.getCurrentRound(), winResult.getWinnerId(), fan, scores, winType);
		ScoreRepository.getInstance().saveRound(table);
		sendRoundResult(table, winResult.getWinnerId(), fan, scores, winType, winResult.getWinTile());

		logger.info("麻将胡牌, table: {}, round: {}, winner: {}, fan: {}, type: {}, scores: {}",
				table.getTableId(), table.getCurrentRound(), winResult.getWinnerId(), fan, winType,
				Arrays.toString(scores));

		table.upNextState(TableState.TABLE_OVER);
	}

	/** 确定胡牌方式 */
	private static String determineWinType(MjWinResult winResult) {
		if (winResult.isZiMo()) return "ziMo";
		if (winResult.isGangShangKaiHua()) return "gangShangHua";
		if (winResult.isQiangGangHu()) return "qiangGangHu";
		if (winResult.isHaiDi()) return "haiDi";
		return "dianPao";
	}

	// ======================== 结算通知 ========================

	/** 发送单局结算通知 */
	private static void sendRoundResult(MjTable table, int winnerSeat, int fan, int[] scores, String winType) {
		sendRoundResult(table, winnerSeat, fan, scores, winType, 0);
	}

	private static void sendRoundResult(MjTable table, int winnerSeat, int fan, int[] scores, String winType, int winTile) {
		int seatNum = table.getTableModel().getSeatNum();
		MjTableContext ctx = table.getMjContext();

		GameProto.NotRoundResult.Builder builder = GameProto.NotRoundResult.newBuilder()
				.setRound(table.getCurrentRound())
				.setWinnerSeat(winnerSeat)
				.setFan(fan)
				.setWinTile(winTile)
				.setWinType(ByteString.copyFromUtf8(winType));

		for (int i = 0; i < seatNum; i++) {
			builder.addSeatScores(GameProto.SeatScore.newBuilder().setSeat(i).setScore(scores[i]).build());
		}

		// 每家副露统计
		for (int i = 0; i < seatNum; i++) {
			GameProto.SeatExposed.Builder seatExposed = GameProto.SeatExposed.newBuilder().setSeat(i);
			for (MjExposedSet set : ctx.getExposedSets(i)) {
				GameProto.ExposedInfo.Builder info = GameProto.ExposedInfo.newBuilder()
						.setType(ByteString.copyFromUtf8(set.getType().toName()));
				for (int tileId : set.getTileIds()) info.addTileIds(tileId);
				seatExposed.addExposed(info.build());
			}
			builder.addSeatExposed(seatExposed.build());
		}

		// 每家手牌(结算展示用)
		for (int i = 0; i < seatNum; i++) {
			TableUser u = table.getSeatUser(i);
			GameProto.HandInfo.Builder handBuilder = GameProto.HandInfo.newBuilder().setSeat(i);
			if (u != null) {
				for (Card c : u.getCards()) handBuilder.addHandTiles(c.getId());
			}
			for (MjExposedSet set : ctx.getExposedSets(i)) {
				GameProto.ExposedInfo.Builder info = GameProto.ExposedInfo.newBuilder()
						.setType(ByteString.copyFromUtf8(set.getType().toName()));
				for (int tileId : set.getTileIds()) info.addTileIds(tileId);
				handBuilder.addExposed(info.build());
			}
			builder.addHands(handBuilder.build());
		}

		table.sendTableMessage(builder.build(), GMsg.NOT_ROUND_RESULT);
	}

	/** 发送总结算通知 */
	public static void sendGameResult(MjTable table) {
		GameResult gameResult = table.getGameResult();
		int seatNum = table.getTableModel().getSeatNum();

		GameProto.NotGameResult.Builder builder = GameProto.NotGameResult.newBuilder()
				.setTotalRounds(gameResult.getTotalRounds())
				.setCompletedRounds(gameResult.getCompletedRounds());

		for (int i = 0; i < seatNum; i++) {
			builder.addTotalScores(GameProto.SeatScore.newBuilder()
					.setSeat(i).setScore(gameResult.getTotalScore(i)).build());
		}

		for (GameResult.RoundEntry entry : gameResult.getRoundEntries()) {
			GameProto.RoundSummary.Builder summary = GameProto.RoundSummary.newBuilder()
					.setRound(entry.getRound())
					.setWinnerSeat(entry.getWinnerSeat())
					.setFan(entry.getScore())
					.setWinType(ByteString.copyFromUtf8(entry.getWinType()));
			for (int i = 0; i < seatNum; i++) {
				summary.addSeatScores(GameProto.SeatScore.newBuilder()
						.setSeat(i).setScore(entry.getScores()[i]).build());
			}
			builder.addRounds(summary.build());
		}

		table.sendTableMessage(builder.build(), GMsg.NOT_GAME_RESULT);
	}

	// ======================== 副露区同步 ========================

	/** 广播所有玩家的副露区给客户端 */
	public static void syncExposedSets(MjTable table) {
		MjTableContext ctx = table.getMjContext();
		int seatNum = table.getTableModel().getSeatNum();

		GameProto.NotMjState.Builder notBuilder = GameProto.NotMjState.newBuilder()
				.setOpSeat(-1)
				.setAction(ConstProto.Operation.MJ_PASS)
				.setWallLeft(table.getMjTilePool().remaining());

		for (int i = 0; i < seatNum; i++) {
			for (MjExposedSet set : ctx.getExposedSets(i)) {
				GameProto.OpInfo.Builder opBuilder = GameProto.OpInfo.newBuilder();
				if (set.getType() == MjExposedSet.Type.PENG) {
					opBuilder.setChoice(ConstProto.Operation.MJ_PENG);
				} else if (set.isGang()) {
					opBuilder.setChoice(ConstProto.Operation.MJ_GANG);
				} else {
					opBuilder.setChoice(ConstProto.Operation.MJ_CHI);
				}
				GameProto.CardInfo.Builder cardInfo = GameProto.CardInfo.newBuilder();
				for (int tileId : set.getTileIds()) {
					cardInfo.addCards(GameProto.Card.newBuilder().setValue(tileId).build());
				}
				opBuilder.addOpCards(cardInfo.build());
				notBuilder.addChoice(opBuilder.build());
			}
		}

		table.sendTableMessage(notBuilder.build(), GMsg.MJ_TILE_NOT);
	}

	/** 广播麻将操作 */
	public static void broadcastMjAction(MjTable table, int seat, int tileId, ConstProto.Operation action) {
		GameProto.NotMjState not = GameProto.NotMjState.newBuilder()
				.setOpSeat(seat).setTileId(tileId)
				.setAction(action)
				.setWallLeft(table.getMjTilePool().remaining()).build();
		table.sendTableMessage(not, GMsg.MJ_TILE_NOT);
	}

	// ======================== 回放 ========================

	/** 写入回放最终状态 */
	private static void writeFinalState(MjTable table, MjReplayRecorder replay) {
		MjTableContext ctx = table.getMjContext();
		int seatNum = table.getTableModel().getSeatNum();
		Map<Integer, List<Integer>> finalHands = new HashMap<>();
		Map<Integer, List<MjExposedSet>> exposedMap = new HashMap<>();
		for (int i = 0; i < seatNum; i++) {
			TableUser u = table.getSeatUser(i);
			if (u != null) {
				finalHands.put(i, u.getCards().stream()
						.mapToInt(Card::getId).boxed().collect(Collectors.toList()));
			}
			exposedMap.put(i, ctx.getExposedSets(i));
		}
		replay.writeFinalState(finalHands, exposedMap, table.getMjTilePool().remaining());
	}
}
