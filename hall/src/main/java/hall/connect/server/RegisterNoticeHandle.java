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
@ProcessType(MessageId.REGISTER_NOTICE)
public class RegisterNoticeHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int aLong, Message msg, int mapId, long sequence) {
		Hall.getInstance().execute(() -> Hall.getInstance().getServerManager().connectToSever(
				((ModelProto.NotRegisterInfo) msg).getServersList(),
				Hall.getInstance().getServerId(), Hall.getInstance().getServerInfo().getIpConfig().toStringUtf8(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Hall));
		return true;
	}
}
