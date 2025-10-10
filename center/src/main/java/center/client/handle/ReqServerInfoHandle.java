package center.client.handle;

import java.util.List;

import center.Center;
import center.client.CenterClient;
import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.client.handler.ClientHandler;
import net.handler.Handler;
import proto.ModelProto;
import utils.ServerClientManager;

/**
 * 获取 服务信息请求
 */
@ProcessType(CMsg.REQ_SERVER)
public class ReqServerInfoHandle implements Handler {

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
		sender.sendMessage(clientId, CMsg.ACK_SERVER, mapId, 0, ack.build(), sequence);
		return true;
	}
}
