package room.client;

import msg.registor.enums.ServerType;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import proto.ModelProto;
import room.Room;

//只有gate过来
public class RoomClient extends ClientHandler {

	private ModelProto.ServerInfo serverInfo;

	public RoomClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent(client -> {

			ServerType serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				Room.getInstance().getServerClientManager().removeServerClient(serverType, serverInfo.getServerId());
			}
		});
	}

	public void setServerInfo(ModelProto.ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}
}
