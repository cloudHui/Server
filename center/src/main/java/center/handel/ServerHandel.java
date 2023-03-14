package center.handel;


import java.util.List;

import center.client.CenterClient;
import center.manager.ServerManager;
import msg.MessageHandel;
import msg.ServerType;
import net.handler.Handler;
import net.message.TCPMessage;
import net.safe.Safe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

public class ServerHandel {

	private final static Logger logger = LoggerFactory.getLogger(ServerHandel.class);

	/**
	 * 心跳
	 */
	public final static Handler<ModelProto.ReqHeart> HEART_HANDLER = (sender, sequence, req, mapId) -> {
		long now = System.currentTimeMillis();
		int serverType = req.getServerType();
		ModelProto.AckHeart.Builder ack = ModelProto.AckHeart.newBuilder();
		ack.setReqTime(now);
		ack.setServerType(ServerType.Center.getServerType());
		sender.sendMessage(MessageHandel.HEART_ACK, ack.build(), null);
		logger.error("server:{}, heart req", ServerType.get(serverType));
		return true;
	};

	/**
	 * 注册请求
	 */
	public final static Handler<ModelProto.ReqRegister> REGISTER_HANDLER = (sender, sequence, req, mapId) -> {
		ServerManager manager = ServerManager.getInstance();
		ModelProto.ServerInfo serverInfo = req.getServerInfo();
		ServerType serverType = ServerType.get(serverInfo.getServerType());
		if (serverType == null) {
			return true;
		}
		int serverId = serverInfo.getServerId();
		CenterClient serverClient = manager.getServerClient(serverType, serverId);
		if (serverClient != null) {
			manager.removeServerClient(serverType, serverId);
			//链接设置不安全然后被关闭
			serverClient.setSafe((Safe<CenterClient, TCPMessage>) (client, msg) -> false);
		}
		serverClient = (CenterClient) sender;
		serverClient.setServerInfo(serverInfo);
		manager.addServerClient(serverType, serverClient, serverId);

		ModelProto.AckRegister.Builder ackRegister = ModelProto.AckRegister.newBuilder();
		ackRegister.setServerInfo(serverInfo);
		sender.sendMessage(Math.toIntExact(sequence), MessageHandel.ACK_REGISTER, ackRegister.build(), null);
		if (serverType != ServerType.Gate) {
			//向 gate 同步 其他服务信息
			List<CenterClient> typeServer = manager.getAllTypeServer(ServerType.Gate);
			if (typeServer != null && !typeServer.isEmpty()) {
				for (CenterClient client : typeServer) {
					ModelProto.NotRegisterInfo.Builder change = ModelProto.NotRegisterInfo.newBuilder();
					change.addServers(serverInfo);
					client.sendMessage(MessageHandel.REGISTER_NOTICE, change.build(), null);
				}
			}
		}
		return true;
	};

	/**
	 * 获取 服务信息请求
	 */
	public final static Handler<ModelProto.ReqServerInfo> SERVER_INFO_HANDLER = (sender, sequence, req, mapId) -> {
		ServerManager manager = ServerManager.getInstance();
		List<Integer> serverTypeList = req.getServerTypeList();
		ModelProto.AckServerInfo.Builder ack = ModelProto.AckServerInfo.newBuilder();
		ServerType server;
		List<CenterClient> allServerClient;
		for (int serverType : serverTypeList) {
			server = ServerType.get(serverType);
			if (server != null) {
				allServerClient = manager.getAllTypeServer(server);
				if (allServerClient != null && !allServerClient.isEmpty()) {
					for (CenterClient client : allServerClient) {
						ack.addServers(client.getServerInfo());
					}
				}
			}
		}
		sender.sendMessage(MessageHandel.ACK_SERVER, ack.build(), null);
		return true;
	};
}
