package gate.handel.server;

import java.util.List;

import gate.Gate;
import gate.connect.ConnectProcessor;
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

	static boolean connectToSever(List<ModelProto.ServerInfo> serverInfos) {
		if (serverInfos == null || serverInfos.isEmpty()) {
			return true;
		}
		Gate instance = Gate.getInstance();
		ServerManager serverManager = instance.getServerManager();
		String[] ipConfig;
		int localServerId = instance.getServerId();
		String localInnerIpConfig = instance.getInnerIp() + ":" + instance.getPort();
		for (ModelProto.ServerInfo serverInfo : serverInfos) {
			ipConfig = serverInfo.getIpConfig().toStringUtf8().split(":");
			serverManager.registerToCenter(ipConfig, ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
					ConnectProcessor.HANDLERS, ServerType.Gate, localServerId, localInnerIpConfig);
		}
		return true;
	}
}
