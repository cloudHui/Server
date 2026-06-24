package robot.connect.handle.game;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import net.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GameProto;
import robot.game.RobotGameSession;
import robot.game.handler.RobotSessionHolder;
import utils.manager.ConnectHandle;

/**
 * 进入桌子回复
 * 记录座位号，设置桌子ID
 */
@ProcessClass(GameProto.AckEnterTable.class)
public class AckEnterTableHandler implements ConnectHandle {
	private static final Logger logger = LoggerFactory.getLogger(AckEnterTableHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		if (message instanceof GameProto.AckEnterTable) {
			GameProto.AckEnterTable ack = (GameProto.AckEnterTable) message;
			RobotGameSession session = RobotSessionHolder.getSession();

			// 找到自己的座位
			for (GameProto.Player player : ack.getPlayersList()) {
				if (player.getRoleId() > 0) {
					session.setMySeat(player.getPosition());
					session.setTableId(ack.getTableInfo().getTableId());
					break;
				}
			}

			logger.info("[Robot seat={} tableId={}] AckEnterTable进入桌子成功, players={}",
					session.getMySeat(), ack.getTableInfo().getTableId(), ack.getPlayersCount());
		}
	}
}
