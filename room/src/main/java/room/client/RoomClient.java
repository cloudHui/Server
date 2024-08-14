package room.client;

import msg.ServerType;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import proto.ModelProto;
import room.Room;


public class RoomClient extends ClientHandler {


	private ModelProto.ServerInfo serverInfo;

	public void setServerInfo(ModelProto.ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}

	public RoomClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent(client -> {

			ServerType serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				Room.getInstance().serverClientManager.removeServerClient(serverType, serverInfo.getServerId());
			}
		});
	}
}
