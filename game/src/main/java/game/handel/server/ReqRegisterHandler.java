package game.handel.server;

import game.Game;
import game.client.GameClient;
import msg.Message;
import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

/**
 * 注册服务信息请求
 */
public class ReqRegisterHandler implements Handler<ModelProto.ReqRegister> {

	private final static Logger LOGGER = LoggerFactory.getLogger(ReqRegisterHandler.class);

	private static ReqRegisterHandler instance = new ReqRegisterHandler();

	public static ReqRegisterHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, ModelProto.ReqRegister req, int mapId) {
		ModelProto.ServerInfo serverInfo = req.getServerInfo();
		ServerType serverType = ServerType.get(serverInfo.getServerType());
		if (serverType == null) {
			return true;
		}

		GameClient client = (GameClient) sender;

		client.setServerInfo(serverInfo);

		Game.getInstance().serverClientManager.addServerClient(serverType, (GameClient) sender, serverInfo.getServerId());


		ModelProto.AckRegister.Builder ackRegister = ModelProto.AckRegister.newBuilder();
		ackRegister.setServerInfo(serverInfo);
		sender.sendMessage(Math.toIntExact(aLong), Message.ACK_REGISTER, ackRegister.build(), null);
		return true;
	}
}
