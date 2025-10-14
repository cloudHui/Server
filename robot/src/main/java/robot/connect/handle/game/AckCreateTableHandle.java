package robot.connect.handle.game;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ServerProto;

/**
 * 创建桌子回复
 */
@ProcessType(SMsg.ACK_CREATE_TABLE_MSG)
public class AckCreateTableHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		ServerProto.AckCreateGameTable ack = (ServerProto.AckCreateGameTable) msg;

		return true;
	}
}
