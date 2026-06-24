package robot.connect.handle.game;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;

/**
 * 创建桌子回复
 */
@ProcessType(SMsg.ACK_CREATE_TABLE_MSG)
public class AckCreateTableHandle implements Handler {

	private static final Logger logger = LoggerFactory.getLogger(AckCreateTableHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, long mapId, int sequence) {
		logger.info("AckCreateTable: 桌子创建成功");
		return true;
	}
}
