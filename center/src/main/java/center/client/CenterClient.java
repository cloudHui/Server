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
