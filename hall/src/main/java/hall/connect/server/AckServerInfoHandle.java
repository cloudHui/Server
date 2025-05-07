package hall.connect.server;

import com.google.protobuf.Message;
import hall.Hall;
import hall.connect.ConnectProcessor;
import msg.registor.message.CMsg;
import msg.registor.enums.ServerType;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 注册信息通知
 */
@ProcessType(CMsg.ACK_SERVER)
public class AckServerInfoHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int aLong, Message msg, int mapId, long sequence) {
		Hall.getInstance().execute(() -> Hall.getInstance().getServerManager().connectToSever(
				((ModelProto.AckServerInfo) msg).getServersList(),
				Hall.getInstance().getServerId(),
				Hall.getInstance().getServerInfo().getIpConfig().toStringUtf8(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Hall));
		return true;
	}
}
