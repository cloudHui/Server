package hall.handle.server;

import com.google.protobuf.Message;
import hall.Hall;
import hall.client.HallClient;
import msg.MessageId;
import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 注册服务信息请求
 */
public class ReqRegisterHandler implements Handler {

	private static final ReqRegisterHandler instance = new ReqRegisterHandler();

	public static ReqRegisterHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, int aLong, Message msg, int mapId, long sequence) {

		ModelProto.ReqRegister req = (ModelProto.ReqRegister) msg;
		ModelProto.ServerInfo serverInfo = req.getServerInfo();
		ServerType serverType = ServerType.get(serverInfo.getServerType());
		if (serverType == null) {
			return true;
		}
		HallClient client = (HallClient) sender;

		client.setServerInfo(serverInfo);


		Hall.getInstance().serverClientManager.addServerClient(serverType, client, serverInfo.getServerId());

		ModelProto.AckRegister.Builder ackRegister = ModelProto.AckRegister.newBuilder();
		ackRegister.setServerInfo(Hall.getInstance().getServerInfo());
		sender.sendMessage(Math.toIntExact(aLong), MessageId.ACK_REGISTER, ackRegister.build(),  sequence);
		return true;
	}
}
