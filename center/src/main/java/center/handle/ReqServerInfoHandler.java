package center.handle;

import java.util.List;

import center.Center;
import center.client.CenterClient;
import com.google.protobuf.Message;
import msg.MessageId;
import msg.ServerType;
import net.client.Sender;
import net.client.handler.ClientHandler;
import net.handler.Handler;
import proto.ModelProto;
import utils.ServerClientManager;

/**
 * 获取 服务信息请求
 */
public class ReqServerInfoHandler implements Handler {

	private static final ReqServerInfoHandler instance = new ReqServerInfoHandler();

	public static ReqServerInfoHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, int clientId, Message reqServerInfo, int mapId, long sequence) {
		ModelProto.ReqServerInfo req = (ModelProto.ReqServerInfo) reqServerInfo;
		ServerClientManager manager = Center.getInstance().getServerManager();
		List<Integer> serverTypeList = req.getServerTypeList();
		ModelProto.AckServerInfo.Builder ack = ModelProto.AckServerInfo.newBuilder();
		ServerType server;
		List<ClientHandler> allServerClient;
		CenterClient centerClient;
		for (int serverType : serverTypeList) {
			server = ServerType.get(serverType);
			if (server != null) {
				allServerClient = manager.getAllTypeServer(server);
				if (allServerClient != null && !allServerClient.isEmpty()) {
					for (ClientHandler client : allServerClient) {
						centerClient = (CenterClient) client;
						ack.addServers(centerClient.getServerInfo());
					}
				}
			}
		}
		sender.sendMessage(MessageId.ACK_SERVER, ack.build());
		return true;
	}
}
