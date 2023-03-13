package gate.handel.server;

import gate.Gate;
import msg.ServerType;
import net.client.Sender;
import net.connect.TCPConnect;
import net.handler.Handler;
import proto.ModelProto;
import utils.ServerManager;

/**
 * 注册服务信息回复
 */
public class AckRegisterHandler implements Handler<ModelProto.AckRegister> {

	private static AckRegisterHandler instance = new AckRegisterHandler();

	public static AckRegisterHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, ModelProto.AckRegister req) {
		ServerManager manager = Gate.getInstance().getServerManager();
		ModelProto.ServerInfo serverInfo = req.getServerInfo();
		ServerType serverType = ServerType.get(serverInfo.getServerType());
		if (serverType == null) {
			return true;
		}
		int serverId = serverInfo.getServerId();
		manager.addServerClient(serverType, (TCPConnect) sender, serverId);
		return true;
	}
}
