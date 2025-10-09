package room.connect.handle;

import com.google.protobuf.Message;
import msg.registor.message.CMsg;
import msg.registor.enums.ServerType;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;
import room.Room;
import room.connect.ConnectProcessor;

/**
 * 注册信息通知
 */
@ProcessType(value = CMsg.ACK_SERVER, trans = ModelProto.AckServerInfo.class)
public class AckServerInfoHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		//如果不放到另一个线程去链接会导致线程读写阻塞
		Room.getInstance().execute(() -> Room.getInstance().getServerManager().connectToSever(
				((ModelProto.AckServerInfo) msg).getServersList(),
				Room.getInstance().getServerId(),
				Room.getInstance().getServerInfo().getIpConfig().toStringUtf8(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Room));

		return true;
	}
}
