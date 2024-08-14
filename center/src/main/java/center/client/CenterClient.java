package center.client;

import java.util.List;

import center.Center;
import msg.MessageId;
import msg.ServerType;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.ServerClientManager;


public class CenterClient extends ClientHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(CenterClient.class);

	private ModelProto.ServerInfo serverInfo;

	public CenterClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent(client -> {
			if (serverInfo == null) {
				return;
			}
			ServerType serverType = ServerType.get(serverInfo.getServerType());
			if (serverType == null) {
				return;
			}
			ServerClientManager serverClientManager = Center.getInstance().serverManager;
			serverClientManager.removeServerClient(serverType, serverInfo.getServerId());
			if (serverType != ServerType.Gate) {
				//向 gate 同步 其他服务信息
				List<ClientHandler> typeServer = serverClientManager.getAllTypeServer(ServerType.Gate);
				if (!typeServer.isEmpty()) {
					for (ClientHandler gate : typeServer) {
						ModelProto.NotServerBreak.Builder change = ModelProto.NotServerBreak.newBuilder();
						change.addServers(serverInfo);
						gate.sendMessage(MessageId.BREAK_NOTICE, change.build(), null);
					}
				}
				LOGGER.error("[center server:{} info:{} break]", serverType, serverInfo.toString());
			}
		});

		setSafe((msgId) -> true);
	}

	public ModelProto.ServerInfo getServerInfo() {
		return serverInfo;
	}

	public void setServerInfo(ModelProto.ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}


}
