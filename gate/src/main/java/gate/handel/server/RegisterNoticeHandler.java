package gate.handel.server;

import java.util.List;

import gate.Gate;
import gate.client.ClientProto;
import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;
import utils.ServerManager;

/**
 * 注册信息通知
 */
public class RegisterNoticeHandler implements Handler<ModelProto.NoticeRegisterInfo> {

	private static RegisterNoticeHandler instance = new RegisterNoticeHandler();

	public static RegisterNoticeHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, ModelProto.NoticeRegisterInfo req) {
		return connectToSever(req.getServersList());
	}

	static boolean connectToSever(List<ModelProto.ServerInfo> serversList2) {
		if (serversList2 == null || serversList2.isEmpty()) {
			return true;
		}
		Gate instance = Gate.getInstance();
		ServerManager serverManager = instance.getServerManager();
		String[] ipConfig;
		int localServerId = instance.getServerId();
		String localInnerIpConfig = instance.getInnerIp() + "" + instance.getPort();
		ServerType serverType;
		for (ModelProto.ServerInfo serverInfo : serversList2) {
			ipConfig = serverInfo.getIpConfig().toStringUtf8().split(":");
			serverType = ServerType.get(serverInfo.getServerType());
			serverManager.connect(serverType, ipConfig[0], Integer.parseInt(ipConfig[1]), ClientProto.TRANSFER, ClientProto.PARSER,
					ClientProto.HANDLERS, ServerType.Gate, localServerId, localInnerIpConfig);
		}
		return true;
	}
}
