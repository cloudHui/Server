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
			ServerClientManager manager = Center.getInstance().getServerManager();
			manager.removeServerClient(serverType, serverInfo.getServerId());

			switch (serverType) {
				case Game:
					noticeBreak(manager, serverInfo, ServerType.Gate);
					noticeBreak(manager, serverInfo, ServerType.Room);
					break;
				case Room:
					noticeBreak(manager, serverInfo, ServerType.Gate);
					noticeBreak(manager, serverInfo, ServerType.Hall);
					break;
				case Hall:
					noticeBreak(manager, serverInfo, ServerType.Gate);
					break;
				default:
					break;
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

	/**
	 * 通知服务关闭
	 *
	 * @param serverInfo 断链上来的服务信息
	 * @param serverType 要通知的服务
	 */
	private void noticeBreak(ServerClientManager manager, ModelProto.ServerInfo serverInfo, ServerType serverType) {
		List<ClientHandler> typeServer = manager.getAllTypeServer(serverType);
		if (!typeServer.isEmpty()) {
			for (ClientHandler gate : typeServer) {
				ModelProto.NotServerBreak.Builder change = ModelProto.NotServerBreak.newBuilder();
				change.addServers(serverInfo);
				gate.sendMessage(MessageId.BREAK_NOTICE, change.build());
			}
			LOGGER.error("[center server:{} info:{} break]", serverType, serverInfo.toString());
		}
	}
}
