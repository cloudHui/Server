package lobby.client;

import io.netty.channel.ChannelHandler;
import lobby.Lobby;
import msg.registor.enums.ServerType;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

public class LobbyClient extends ClientHandler {
	private static final Logger logger = LoggerFactory.getLogger(LobbyClient.class);

	private ModelProto.ServerInfo serverInfo;

	public LobbyClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);
		setCloseEvent(this::handleConnectionClose);
	}

	private void handleConnectionClose(ChannelHandler client) {
		logger.info("Lobby 客户端连接关闭, serverInfo: {}",
				serverInfo != null ? serverInfo.getServerId() : "unknown");
		if (serverInfo != null) {
			ServerType serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				Lobby.getInstance().serverClientManager.removeServerClient(serverType, serverInfo.getServerId());
			}
		}
	}

	public void setServerInfo(ModelProto.ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}
}
