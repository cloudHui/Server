package center.handle;

import java.util.List;

import center.client.CenterClient;
import center.manager.ServerManager;
import msg.MessageHandel;
import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

public class ReqServerInfoHandler implements Handler<ModelProto.ReqServerInfo> {

	private static ReqServerInfoHandler instance = new ReqServerInfoHandler();

	public static ReqServerInfoHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, ModelProto.ReqServerInfo req) {
		ServerManager manager = ServerManager.getInstance();
		List<Integer> serverTypeList = req.getServerTypeList();
		ModelProto.AckServerInfo.Builder ack = ModelProto.AckServerInfo.newBuilder();
		ServerType server;
		List<CenterClient> allServerClient;
		for (int serverType : serverTypeList) {
			server = ServerType.get(serverType);
			if (server != null) {
				allServerClient = manager.getAllServerClient(server);
				if (allServerClient != null && !allServerClient.isEmpty()) {
					for (CenterClient client : allServerClient) {
						ack.addServers(client.getServerInfo());
					}
				}
			}
		}
		sender.sendMessage(MessageHandel.CenterMsg.SERVER_REQ.getId(), ack.build(), null);
		return true;
	}
}
