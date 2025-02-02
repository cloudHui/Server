package room.handel;

import com.google.protobuf.Message;
import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;
import room.Room;
import room.connect.ConnectProcessor;

/**
 * 注册信息通知
 */
public class AckServerInfoHandel implements Handler {

	private static final AckServerInfoHandel instance = new AckServerInfoHandel();

	public static AckServerInfoHandel getInstance() {
		return instance;
	}

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
