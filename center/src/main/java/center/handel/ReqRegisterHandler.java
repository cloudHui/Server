package center.handel;

import java.util.List;

import center.Center;
import center.client.CenterClient;
import com.google.protobuf.ByteString;
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
		ServerClientManager manager = Center.getInstance().getServerManager();
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
		ackRegister.setServerInfo(Center.getInstance().getServerInfo());
		sender.sendMessage(MessageId.ACK_REGISTER, ackRegister.build(), sequence);
		switch (serverType) {
			case Game:
				noticeConnect(manager, serverInfo, ServerType.Gate);
				noticeConnect(manager, serverInfo, ServerType.Room);
				break;
			case Room:
				noticeConnect(manager, serverInfo, ServerType.Gate);
				noticeConnect(manager, serverInfo, ServerType.Hall);
				break;
			case Hall:
				noticeConnect(manager, serverInfo, ServerType.Gate);
				break;
			default:
				break;
		}
		return true;
	}

	/**
	 * 通知服务链接
	 *
	 * @param serverInfo 链接上来的服务信息
	 * @param serverType 要通知的服务
	 */
	private void noticeConnect(ServerClientManager manager, ModelProto.ServerInfo serverInfo, ServerType serverType) {
		//向 serverType 同步 其他服务信息
		List<ClientHandler> typeServer = manager.getAllTypeServer(serverType);
		if (typeServer != null && !typeServer.isEmpty()) {
			for (ClientHandler client : typeServer) {
				ModelProto.NotRegisterInfo.Builder change = ModelProto.NotRegisterInfo.newBuilder();
				change.addServers(serverInfo);
				client.sendMessage(MessageId.REGISTER_NOTICE, change.build());
			}
			LOGGER.error("[center server:{} info:{} reqRegister]", serverType, serverInfo.toString());
		}
	}

}
