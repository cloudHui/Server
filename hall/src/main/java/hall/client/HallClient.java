package hall.client;

import hall.Hall;
import msg.ServerType;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import proto.ModelProto;


public class HallClient extends ClientHandler {


	private ModelProto.ServerInfo serverInfo;

	public void setServerInfo(ModelProto.ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}

	public HallClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent(client -> {

			ServerType serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				Hall.getInstance().serverClientManager.removeServerClient(serverType, serverInfo.getServerId());
			}
		});
	}
}
