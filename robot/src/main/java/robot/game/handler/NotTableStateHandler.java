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
 * 桌子状态通知处理器
 * 记录状态变化
 */
@ProcessClass(GameProto.NotTableState.class)
public class NotTableStateHandler implements ConnectHandle {

	private static final Logger logger = LoggerFactory.getLogger(NotTableStateHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		if (!(message instanceof GameProto.NotTableState)) return;

		GameProto.NotTableState not = (GameProto.NotTableState) message;
		RobotGameSession session = RobotSessionHolder.getSession();

		logger.info("[Robot seat={} tableId={}] NotTableState, state={}, stateStart={}, duration={}",
				session.getMySeat(), session.getTableId(),
				not.getState(), not.getStateStart(), not.getStateDuration());
	}
}
