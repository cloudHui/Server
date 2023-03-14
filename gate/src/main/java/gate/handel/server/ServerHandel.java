package gate.handel.server;


import java.util.List;

import gate.Gate;
import gate.connect.ConnectProcessor;
import msg.ServerType;
import net.handler.Handler;
import proto.ModelProto;
import utils.ServerManager;

public class ServerHandel {

	/**
	 * 注册信息回复
	 */
	public final static Handler<ModelProto.AckServerInfo> ACK_SERVER_INFO = (sender, sequence, ack, mapId) ->
			connectToSever(ack.getServersList());

	/**
	 * 注册信息通知
	 */
	public final static Handler<ModelProto.NotRegisterInfo> NOT_REGISTER_INFO = (sender, sequence, ack, mapId) ->
			connectToSever(ack.getServersList());

	/**
	 * 去链接 服务
	 */
	static boolean connectToSever(List<ModelProto.ServerInfo> serverInfos) {
		if (serverInfos == null || serverInfos.isEmpty()) {
			return true;
		}
		Gate instance = Gate.getInstance();
		ServerManager serverManager = instance.getServerManager();
		String[] ipConfig;
		int localServerId = instance.getServerId();
		String localInnerIpConfig = instance.getInnerIp() + ":" + instance.getPort();
		ServerType serverType;
		for (ModelProto.ServerInfo serverInfo : serverInfos) {
			ipConfig = serverInfo.getIpConfig().toStringUtf8().split(":");
			serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				serverManager.registerSever(ipConfig, ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
						ConnectProcessor.HANDLERS, ServerType.Gate, localServerId, localInnerIpConfig, serverType);
			}
		}
		return true;
	}
}
