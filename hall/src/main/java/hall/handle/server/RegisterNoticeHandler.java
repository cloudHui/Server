package hall.handle.server;

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
public class RegisterNoticeHandler implements Handler {

	@Override
	public boolean handler(Sender sender, int aLong, Message registerInfo, int mapId, long sequence) {
		ModelProto.NotRegisterInfo req = (ModelProto.NotRegisterInfo) registerInfo;

		Hall.getInstance().getServerManager().connectToSever(req.getServersList(),
				Hall.getInstance().getServerId(), Hall.getInstance().getServerInfo().getIpConfig().toStringUtf8(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Room);
		return true;
	}
}
