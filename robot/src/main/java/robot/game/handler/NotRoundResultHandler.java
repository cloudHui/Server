package robot.game.handler;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import net.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GameProto;
import robot.game.RobotGameSession;
import tools.manager.ConnectHandle;

/**
 * 单局结算通知处理器
 * 记录每局结算详情（赢家、分数、牌型）
 */
@ProcessClass(GameProto.NotRoundResult.class)
public class NotRoundResultHandler implements ConnectHandle {

	private static final Logger logger = LoggerFactory.getLogger(NotRoundResultHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		if (!(message instanceof GameProto.NotRoundResult)) return;

		GameProto.NotRoundResult result = (GameProto.NotRoundResult) message;
		RobotGameSession session = RobotSessionHolder.getSession();

		// 构建每家分数
		StringBuilder scores = new StringBuilder("[");
		for (int i = 0; i < result.getSeatScoresCount(); i++) {
			if (i > 0) scores.append(", ");
			scores.append("座").append(result.getSeatScores(i).getSeat())
					.append(":").append(result.getSeatScores(i).getScore());
		}
		scores.append("]");

		logger.info("[Robot seat={} tableId={}] NotRoundResult单局结算, round={}, winnerSeat={}, winType={}, fan={}, scores={}, 手牌数={}",
				session.getMySeat(), session.getTableId(),
				result.getRound(), result.getWinnerSeat(),
				result.getWinType().toStringUtf8(), result.getFan(),
				scores, session.getHandSize());
	}
}
