package robot.game.handler;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import net.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ConstProto;
import proto.GameProto;
import robot.game.RobotGameSession;
import utils.manager.ConnectHandle;

/**
 * 麻将状态通知处理器
 * 处理摸牌、出牌、碰杠吃胡等状态变化
 */
@ProcessClass(GameProto.NotMjState.class)
public class NotMjStateHandler implements ConnectHandle {

	private static final Logger logger = LoggerFactory.getLogger(NotMjStateHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		if (!(message instanceof GameProto.NotMjState)) return;

		GameProto.NotMjState not = (GameProto.NotMjState) message;
		RobotGameSession session = RobotSessionHolder.getSession();

		ConstProto.Operation action = not.getAction();
		int opSeat = not.getOpSeat();
		int tileId = not.getTileId();
		int wallLeft = not.getWallLeft();

		switch (action) {
			case DRAW:
				if (opSeat == session.getMySeat() || opSeat == -1) {
					session.addCard(tileId);
					logger.info("[Robot seat={} tableId={}] 摸牌, tile={}, 牌墙剩余={}, 手牌数={}, 手牌={}",
							session.getMySeat(), session.getTableId(), tileId, wallLeft,
							session.getHandSize(), session.getHandCards());
				} else {
					logger.debug("[Robot seat={}] 他人摸牌, opSeat={}, tile={}", session.getMySeat(), opSeat, tileId);
				}
				break;

			case DISCARD:
				if (opSeat == session.getMySeat()) {
					session.removeCard(tileId);
					logger.info("[Robot seat={} tableId={}] 出牌, tile={}, 牌墙剩余={}, 手牌数={}, 手牌={}",
							session.getMySeat(), session.getTableId(), tileId, wallLeft,
							session.getHandSize(), session.getHandCards());
				} else {
					logger.info("[Robot seat={}] 他人出牌, opSeat={}, tile={}, 牌墙剩余={}",
							session.getMySeat(), opSeat, tileId, wallLeft);
				}
				break;

			case MJ_PENG:
				logger.info("[Robot seat={} tableId={}] 碰, opSeat={}, tile={}, 牌墙剩余={}",
						session.getMySeat(), session.getTableId(), opSeat, tileId, wallLeft);
				break;

			case MJ_GANG:
				logger.info("[Robot seat={} tableId={}] 杠, opSeat={}, tile={}, 牌墙剩余={}",
						session.getMySeat(), session.getTableId(), opSeat, tileId, wallLeft);
				break;

			case MJ_CHI:
				logger.info("[Robot seat={} tableId={}] 吃, opSeat={}, tile={}, 牌墙剩余={}",
						session.getMySeat(), session.getTableId(), opSeat, tileId, wallLeft);
				break;

			case MJ_HU:
				logger.info("[Robot seat={} tableId={}] 胡牌, opSeat={}, tile={}",
						session.getMySeat(), session.getTableId(), opSeat, tileId);
				break;

			default:
				logger.debug("[Robot seat={}] MjState: action={}, opSeat={}, tile={}, wallLeft={}",
						session.getMySeat(), action, opSeat, tileId, wallLeft);
				break;
		}
	}
}
