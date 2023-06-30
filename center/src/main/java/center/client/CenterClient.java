package center.client;

import center.Center;
import msg.ServerType;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;
import net.safe.Safe;
import proto.ModelProto;


public class CenterClient extends ClientHandler<CenterClient, TCPMessage> {

	private ModelProto.ServerInfo serverInfo;

	public CenterClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<CenterClient>) client -> {
			if (serverInfo == null) {
				return;
			}
			ServerType serverType = ServerType.get(serverInfo.getServerType());
			if (serverType == null) {
				return;
			}
			Center.getInstance().serverManager.removeServerClient(serverType, serverInfo.getServerId());
		});

		setSafe((Safe<CenterClient, TCPMessage>) (client, msg) -> true);
	}

	public ModelProto.ServerInfo getServerInfo() {
		return serverInfo;
	}

	public void setServerInfo(ModelProto.ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}
}
