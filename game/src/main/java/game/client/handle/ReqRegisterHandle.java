package game.client.handle;

import com.google.protobuf.Message;
import game.Game;
import game.client.GameClient;
import msg.MessageId;
import msg.ServerType;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * gate注册服务信息请求
 */
@ProcessType(value = MessageId.REQ_REGISTER)
public class ReqRegisterHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int roleId, Message reqRegister, int mapId, long sequence) {
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
		sender.sendMessage(MessageId.ACK_REGISTER, ackRegister.build(), sequence);
		return true;
	}
}
