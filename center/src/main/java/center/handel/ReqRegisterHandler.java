package center.handel;

import java.util.List;

import center.client.CenterClient;
import center.manager.ServerManager;
import msg.MessageHandel;
import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import net.message.TCPMessage;
import net.safe.Safe;
import proto.ModelProto;

/**
 * 注册服务信息请求
 */
public class ReqRegisterHandler implements Handler<ModelProto.ReqRegister> {

	private static ReqRegisterHandler instance = new ReqRegisterHandler();

	public static ReqRegisterHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, ModelProto.ReqRegister req) {
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
		if (serverType != ServerType.Gate) {
			//向 gate 同步 其他服务信息
			List<CenterClient> typeServer = manager.getAllTypeServer(ServerType.Gate);
			if (typeServer != null && !typeServer.isEmpty()) {
				for (CenterClient client : typeServer) {
					ModelProto.NoticeRegisterInfo.Builder change = ModelProto.NoticeRegisterInfo.newBuilder();
					change.addServers(serverInfo);
					client.sendMessage(MessageHandel.REGISTER_NOTICE, change.build(), null);
				}
			}
		}
		return true;
	}
}