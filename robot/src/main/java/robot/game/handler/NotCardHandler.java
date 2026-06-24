package robot.game.handler;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import msg.registor.message.GMsg;
import net.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GameProto;
import robot.game.RobotGameSession;
import utils.manager.ConnectHandle;
import utils.manager.HandleManager;

/**
 * 发牌通知处理器
 * 收到发牌后记录手牌，注册操作发送器
 */
@ProcessClass(GameProto.NotCard.class)
public class NotCardHandler implements ConnectHandle {

	private static final Logger logger = LoggerFactory.getLogger(NotCardHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		if (!(message instanceof GameProto.NotCard)) return;

		GameProto.NotCard notCard = (GameProto.NotCard) message;
		RobotGameSession session = RobotSessionHolder.getSession();

		// 找到自己的roleId（第一个有正面牌值的牌组就是自己）
		int roleId = 0;
		for (GameProto.NCardsInfo nCards : notCard.getNCardsList()) {
			if (nCards.getCardsCount() > 0 && nCards.getCards(0).getValue() > 0) {
				roleId = nCards.getRoleId();
				break;
			}
		}

		if (roleId == 0) {
			logger.error("[Robot] NotCard: 无法确定roleId, nCardsList.size={}", notCard.getNCardsList().size());
			return;
		}

		session.initHand(notCard.getNCardsList(), roleId);

		// 注册操作发送器
		Sender capturedSender = handler;
		session.setOpSender((tableId, op, seq) -> {
			GameProto.ReqOp reqOp = GameProto.ReqOp.newBuilder().setOp(op).build();
			HandleManager.sendMsg(GMsg.REQ_OP, reqOp, capturedSender,
					robot.connect.ConnectProcessor.PARSER, seq, false);
		});

		logger.info("[Robot seat={} tableId={}] NotCard收到发牌, roleId={}, 手牌数={}, cards={}",
				session.getMySeat(), session.getTableId(), roleId, session.getHandSize(), session.getHandCards());
	}
}
