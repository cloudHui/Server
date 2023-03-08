package router.handle;

import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import net.message.TCPMessage;
import net.safe.Safe;
import proto.ModelProto;
import router.client.RouterClient;
import router.manager.ServerManager;

public class RegisterEventHandler implements Handler<ModelProto.RegisterNotice> {

	private static RegisterEventHandler instance = new RegisterEventHandler();

	public static RegisterEventHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, ModelProto.RegisterNotice req) {
		ServerManager manager = ServerManager.getInstance();
		ServerType serverType = ServerType.get(req.getServerType());
		if (serverType == null) {
			return false;
		}
		RouterClient serverClient = manager.getServerClient(serverType, req.getServerId());
		if (serverClient != null) {
			//链接设置不安全然后被关闭
			serverClient.setSafe((Safe<RouterClient, TCPMessage>) (client, msg) -> false);
		}
		serverClient = (RouterClient) sender;
		serverClient.setIpConfig(req.getIpConfig());
		manager.addServerClient(serverType, serverClient, req.getServerId());
		return true;
	}
}
