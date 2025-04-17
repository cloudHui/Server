package hall.connect.server;

import com.google.protobuf.Message;
import hall.Hall;
import hall.connect.ConnectProcessor;
import msg.MessageId;
import msg.ServerType;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 注册信息通知
 */
@ProcessType(MessageId.ACK_SERVER)
public class AckServerInfoHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int aLong, Message ackServerInfo, int mapId, long sequence) {

		ModelProto.AckServerInfo req = (ModelProto.AckServerInfo) ackServerInfo;
		Hall.getInstance().getServerManager().connectToSever(req.getServersList(),
				Hall.getInstance().getServerId(),
				Hall.getInstance().getServerInfo().getIpConfig().toStringUtf8(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Hall);
		return true;
	}
}
