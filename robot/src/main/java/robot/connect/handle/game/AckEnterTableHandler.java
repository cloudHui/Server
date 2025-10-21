package robot.connect.handle.game;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import net.connect.handle.ConnectHandler;
import proto.GameProto;
import utils.manager.ConnectHandle;

/**
 * 登录回复
 */
@ProcessClass(GameProto.AckEnterTable.class)
public class AckEnterTableHandler implements ConnectHandle {

	@Override
	public void handle(Message message, ConnectHandler handler, int sequence, int transId) {
		if (message instanceof GameProto.AckEnterTable) {
			GameProto.AckEnterTable ack = (GameProto.AckEnterTable) message;
			LOGGER.error("AckEnterTable:{}", ack.toString());
			//Todo 等待开始
		}
	}
}
