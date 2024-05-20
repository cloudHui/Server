package center.handel;

import java.util.List;

import center.Center;
import center.client.CenterClient;
import msg.Message;
import msg.ServerType;
import net.client.Sender;
import net.client.handler.ClientHandler;
import net.handler.Handler;
import proto.ModelProto;
import utils.ServerClientManager;

/**
 * 获取 服务信息请求
 */
public class ReqServerInfoHandler implements Handler<ModelProto.ReqServerInfo> {

	private static final ReqServerInfoHandler instance = new ReqServerInfoHandler();

	public static ReqServerInfoHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, ModelProto.ReqServerInfo req, int mapId) {
		ServerClientManager manager = Center.getInstance().serverManager;
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
		sender.sendMessage(Message.ACK_SERVER, ack.build(), null);
		return true;
	}
}
