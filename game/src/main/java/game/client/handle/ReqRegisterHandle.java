package game.client.handle;

import com.google.protobuf.Message;
import game.Game;
import game.client.GameClient;
import msg.registor.message.CMsg;
import msg.registor.enums.ServerType;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * gate注册服务信息请求
 */
@ProcessType(value = CMsg.REQ_REGISTER)
public class ReqRegisterHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message reqRegister, int mapId, long sequence) {
		ModelProto.ReqRegister req = (ModelProto.ReqRegister) reqRegister;
		ModelProto.ServerInfo serverInfo = req.getServerInfo();
		ServerType serverType = ServerType.get(serverInfo.getServerType());
		if (serverType == null) {
			return true;
		}

		GameClient client = (GameClient) sender;

		client.setServerInfo(serverInfo);

		Game.getInstance().getServerClientManager().addServerClient(serverType, (GameClient) sender, serverInfo.getServerId());


		ModelProto.AckRegister.Builder ackRegister = ModelProto.AckRegister.newBuilder();
		ackRegister.setServerInfo(Game.getInstance().getServerInfo());
		sender.sendMessage(clientId, CMsg.ACK_REGISTER, mapId, 0, ackRegister.build(), sequence);
		return true;
	}
}
