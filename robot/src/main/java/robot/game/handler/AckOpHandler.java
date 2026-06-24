package robot.game.handler;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import net.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GameProto;
import robot.game.RobotGameSession;
import utils.manager.ConnectHandle;

/**
 * 操作确认通知处理器
 * 收到操作确认后记录结果
 */
@ProcessClass(GameProto.AckOp.class)
public class AckOpHandler implements ConnectHandle {

	private static final Logger logger = LoggerFactory.getLogger(AckOpHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		if (!(message instanceof GameProto.AckOp)) return;

		GameProto.AckOp ack = (GameProto.AckOp) message;
		RobotGameSession session = RobotSessionHolder.getSession();

		logger.info("[Robot seat={} tableId={}] AckOp确认, opId={}, choice={}, 手牌数={}",
				session.getMySeat(), session.getTableId(),
				ack.getOpId(), ack.getOp().getChoice(), session.getHandSize());
	}
}
