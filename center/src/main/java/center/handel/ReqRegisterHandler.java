package center.handel;

import java.util.List;

import center.Center;
import center.client.CenterClient;
import com.google.protobuf.Message;
import msg.MessageId;
import msg.ServerType;
import net.client.Sender;
import net.client.handler.ClientHandler;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.ServerClientManager;

/**
 * 注册服务信息请求
 */
public class ReqRegisterHandler implements Handler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReqRegisterHandler.class);

	private static final ReqRegisterHandler instance = new ReqRegisterHandler();

	public static ReqRegisterHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, int clientId, Message reqRegister, int mapId, long sequence) {
		ModelProto.ReqRegister req = (ModelProto.ReqRegister) reqRegister;
		ServerClientManager manager = Center.getInstance().serverManager;
		ModelProto.ServerInfo serverInfo = req.getServerInfo();
		ServerType serverType = ServerType.get(serverInfo.getServerType());
		if (serverType == null) {
			return true;
		}
		int serverId = serverInfo.getServerId();
		CenterClient serverClient = (CenterClient) manager.getServerClient(serverType, serverId);
		if (serverClient != null) {
			manager.removeServerClient(serverType, serverId);
			//链接设置不安全然后被关闭
			serverClient.setSafe((msgId) -> false);
		}
		serverClient = (CenterClient) sender;
		serverClient.setServerInfo(serverInfo);
		manager.addServerClient(serverType, serverClient, serverId);

		ModelProto.AckRegister.Builder ackRegister = ModelProto.AckRegister.newBuilder();
		ackRegister.setServerInfo(serverInfo);
		sender.sendMessage(MessageId.ACK_REGISTER, ackRegister.build(), null);
		if (serverType != ServerType.Gate) {
			//向 gate 同步 其他服务信息
			List<ClientHandler> typeServer = manager.getAllTypeServer(ServerType.Gate);
			if (typeServer != null && !typeServer.isEmpty()) {
				for (ClientHandler client : typeServer) {
					ModelProto.NotRegisterInfo.Builder change = ModelProto.NotRegisterInfo.newBuilder();
					change.addServers(serverInfo);
					client.sendMessage(MessageId.REGISTER_NOTICE, change.build(), null);
				}
			}
			LOGGER.error("[center server:{} info:{} reqRegister]", serverType, serverInfo.toString());
		}
		return true;
	}
}
