package room.connect.handle;

import com.google.protobuf.Message;
import msg.MessageId;
import msg.ServerType;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;
import room.Room;
import room.connect.ConnectProcessor;

/**
 * 注册信息通知
 */
@ProcessType(MessageId.ACK_SERVER)
public class AckServerInfoHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int aLong, Message msg, int mapId, long sequence) {
		ModelProto.AckServerInfo ack = (ModelProto.AckServerInfo) msg;
		Room.getInstance().getServerManager().connectToSever(ack.getServersList(),
				Room.getInstance().getServerId(),
				Room.getInstance().getServerInfo().getIpConfig().toStringUtf8(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Room);
		return true;
	}
}
