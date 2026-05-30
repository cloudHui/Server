package robot.connect.handle.game;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import net.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GameProto;
import utils.manager.ConnectHandle;

/**
 * 登录回复
 */
@ProcessClass(GameProto.AckEnterTable.class)
public class AckEnterTableHandler implements ConnectHandle {
	private static final Logger logger = LoggerFactory.getLogger(AckEnterTableHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		if (message instanceof GameProto.AckEnterTable) {
			GameProto.AckEnterTable ack = (GameProto.AckEnterTable) message;
			logger.info("AckEnterTable:{}", ack.toString());
			//Todo 等待开始
		}
	}
}
