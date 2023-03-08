package center.handle;

import center.client.CenterClient;
import center.manager.ServerManager;
import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import net.message.TCPMessage;
import net.safe.Safe;
import proto.ModelProto;

public class RegisterEventHandler implements Handler<ModelProto.ReqRegisterNotice> {

	private static RegisterEventHandler instance = new RegisterEventHandler();

	public static RegisterEventHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, ModelProto.ReqRegisterNotice req) {
		ServerManager manager = ServerManager.getInstance();
		ModelProto.ServerInfo serverInfo = req.getServerInfo();
		ServerType serverType = ServerType.get(serverInfo.getServerType());
		if (serverType == null) {
			return false;
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
		return true;
	}
}
