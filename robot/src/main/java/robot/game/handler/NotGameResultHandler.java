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
 * 总结算通知处理器
 * 记录整场游戏结果，重置会话
 */
@ProcessClass(GameProto.NotGameResult.class)
public class NotGameResultHandler implements ConnectHandle {

	private static final Logger logger = LoggerFactory.getLogger(NotGameResultHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		if (!(message instanceof GameProto.NotGameResult)) return;

		GameProto.NotGameResult result = (GameProto.NotGameResult) message;
		RobotGameSession session = RobotSessionHolder.getSession();

		// 构建总分
		StringBuilder totalScores = new StringBuilder("[");
		for (int i = 0; i < result.getTotalScoresCount(); i++) {
			if (i > 0) totalScores.append(", ");
			totalScores.append("座").append(i)
					.append(":").append(result.getTotalScores(i).getScore());
		}
		totalScores.append("]");

		logger.info("[Robot seat={} tableId={}] NotGameResult总结算, totalRounds={}, completedRounds={}, totalScores={}",
				session.getMySeat(), session.getTableId(),
				result.getTotalRounds(), result.getCompletedRounds(), totalScores);

		session.reset();
	}
}
