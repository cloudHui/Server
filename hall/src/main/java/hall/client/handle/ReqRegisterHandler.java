package hall.client.handle;

import com.google.protobuf.Message;
import hall.Hall;
import hall.client.HallClient;
import msg.registor.message.CMsg;
import msg.registor.enums.ServerType;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 注册服务信息请求
 */
@ProcessType(CMsg.REQ_REGISTER)
public class ReqRegisterHandler implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {

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
		sender.sendMessage(clientId, CMsg.ACK_REGISTER, mapId, 0, ackRegister.build(), sequence);
		return true;
	}
}
