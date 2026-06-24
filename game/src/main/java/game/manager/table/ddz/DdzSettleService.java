package game.manager.table.ddz;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import game.manager.table.DdzTable;
import game.manager.table.GameResult;
import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.cards.Card;
import game.manager.table.replay.DdzReplayRecorder;
import game.manager.table.replay.ReplayRecorder;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import proto.GameProto;

/**
 * 斗地主结算服务
 * 处理单局结束结算、总结算通知
 */
public final class DdzSettleService {

	private static final Logger logger = LoggerFactory.getLogger(DdzSettleService.class);

	private DdzSettleService() {}

	/**
	 * 结束游戏：计算得分、记录回放、发送结算通知
	 * @param table  桌子
	 * @param winner 赢家
	 */
	static void finishGame(DdzTable table, TableUser winner) {
		DdzTableContext ctx = table.getDdz();
		int landlordSeat = ctx.getLandlordSeat();
		TableUser landlordUser = table.getSeatUser(landlordSeat);
		int landlordUserId = landlordUser != null ? landlordUser.getUserId() : 0;

		boolean landlordWin = winner.getSeated() == landlordSeat;
		int winTeam = landlordWin ? 0 : 1;
		boolean spring = landlordWin && !ctx.isFarmerEverPlayed();
		boolean antiSpring = !landlordWin && ctx.getLandlordPlayCount() <= 1;

		int settleFactor = ctx.getBaseScore() * ctx.getRobMultiplier();
		if (spring) settleFactor *= 2;
		if (antiSpring) settleFactor *= 2;

		int seatNum = table.getTableModel().getSeatNum();
		int[] scores = calcScores(seatNum, landlordSeat, landlordWin, settleFactor);

		String winType = spring ? "spring" : (antiSpring ? "antiSpring" : "normal");
		table.getGameResult().addRound(table.getCurrentRound(), winner.getSeated(), settleFactor, scores, winType);

		saveReplay(table, winner, settleFactor, winType, scores);
		sendResultMessage(table, winner, rPlayers(table), landlordUserId, winTeam, ctx, spring, antiSpring, settleFactor, seatNum, scores, winType);

		table.upNextStateWithTime(TableState.TABLE_OVER, System.currentTimeMillis());
	}

	/** 发送DDZ总结算通知(多局汇总) */
	public static void sendGameResult(Table table) {
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

	// ======================== 内部方法 ========================

	/** 计算每家得分 */
	private static int[] calcScores(int seatNum, int landlordSeat, boolean landlordWin, int settleFactor) {
		int[] scores = new int[seatNum];
		for (int i = 0; i < seatNum; i++) {
			if (i == landlordSeat) {
				scores[i] = landlordWin ? settleFactor * (seatNum - 1) : -settleFactor * (seatNum - 1);
			} else {
				scores[i] = landlordWin ? -settleFactor : settleFactor;
			}
		}
		return scores;
	}

	/** 保存回放 */
	private static void saveReplay(DdzTable table, TableUser winner, int settleFactor, String winType, int[] scores) {
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay instanceof DdzReplayRecorder) {
			replay.writeSettlement(winner.getSeated(), settleFactor, winType, scores);
			replay.save();
		}
	}

	/** 构建RPlayer列表 */
	private static List<GameProto.RPlayer> rPlayers(DdzTable table) {
		List<GameProto.RPlayer> rPlayers = new ArrayList<>();
		for (TableUser u : table.getSeatUsers().values()) {
			GameProto.RPlayer.Builder rp = GameProto.RPlayer.newBuilder().setRoleId(u.getUserId());
			for (Card c : u.getCards()) {
				rp.addCards(GameProto.Card.newBuilder().setValue(c.getId()).build());
			}
			rPlayers.add(rp.build());
		}
		return rPlayers;
	}

	/** 发送结算消息 */
	private static void sendResultMessage(DdzTable table, TableUser winner, List<GameProto.RPlayer> rPlayers,
			int landlordUserId, int winTeam, DdzTableContext ctx, boolean spring, boolean antiSpring,
			int settleFactor, int seatNum, int[] scores, String winType) {
		try {
			byte[] payload = DdzResultEncoder.encodeNotResultExtended(
					winner.getUserId(), rPlayers, landlordUserId, winTeam,
					ctx.getBaseScore(), ctx.getRobMultiplier(), spring, antiSpring, settleFactor);
			table.sendTableMessageRaw(GMsg.NOT_RESULT, payload);
		} catch (IOException e) {
			logger.error("encode NotResult failed table:{}", table.getTableId(), e);
			GameProto.NotResult.Builder b = GameProto.NotResult.newBuilder().setWinner(winner.getUserId());
			for (GameProto.RPlayer rp : rPlayers) b.addRPlayers(rp);
			table.sendTableMessage(b.build(), GMsg.NOT_RESULT);
		}

		if (table.isMultiRound()) {
			GameProto.NotRoundResult.Builder roundResult = GameProto.NotRoundResult.newBuilder()
					.setRound(table.getCurrentRound())
					.setWinnerSeat(winner.getSeated())
					.setFan(settleFactor)
					.setWinType(ByteString.copyFromUtf8(winType));
			for (int i = 0; i < seatNum; i++) {
				roundResult.addSeatScores(GameProto.SeatScore.newBuilder()
						.setSeat(i).setScore(scores[i]).build());
			}
			table.sendTableMessage(roundResult.build(), GMsg.NOT_ROUND_RESULT);
		}
	}
}
