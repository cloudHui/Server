package robot.game.handler;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import net.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GameProto;
import robot.game.RobotGameSession;
import tools.manager.ConnectHandle;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作提示通知处理器
 * 收到出牌/叫分/抢地主等提示后，自动选择操作并发送
 */
@ProcessClass(GameProto.NotOperation.class)
public class NotOpHandler implements ConnectHandle {

	private static final Logger logger = LoggerFactory.getLogger(NotOpHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		if (!(message instanceof GameProto.NotOperation)) return;

		GameProto.NotOperation notOp = (GameProto.NotOperation) message;
		RobotGameSession session = RobotSessionHolder.getSession();

		// 记录可选操作
		session.setChoices(notOp.getChoiceList());

		// 打印可选操作详情
		List<String> choiceNames = new ArrayList<>();
		for (GameProto.OpInfo op : notOp.getChoiceList()) {
			choiceNames.add(op.getChoice().name());
		}

		logger.info("[Robot seat={} tableId={}] NotOp收到操作提示, opSeat={}, 可选操作={}, 手牌数={}, 手牌={}",
				session.getMySeat(), session.getTableId(), notOp.getOpSeat(),
				choiceNames, session.getHandSize(), session.getHandCards());

		// 延迟一小段时间后自动操作（模拟思考）
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		session.autoChooseAndSend();
	}
}
