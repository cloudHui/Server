package hall.handel.server;

import com.google.protobuf.Message;
import hall.Hall;
import hall.connect.ConnectProcessor;
import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 注册信息通知
 */
public class RegisterNoticeHandler implements Handler {

	private static final RegisterNoticeHandler instance = new RegisterNoticeHandler();

	public static RegisterNoticeHandler getInstance() {
		return instance;
	}

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
