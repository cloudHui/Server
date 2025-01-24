package game.handel.server;

import com.google.protobuf.Message;
import game.Game;
import game.client.GameClient;
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
		ackRegister.setServerInfo(serverInfo);
		sender.sendMessage(MessageId.ACK_REGISTER, ackRegister.build(), sequence);
		return true;
	}
}
