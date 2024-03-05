package room.client;

import msg.ServerType;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;
import proto.ModelProto;
import room.Room;


public class RoomClient extends ClientHandler<RoomClient, TCPMessage> {


	private ModelProto.ServerInfo serverInfo;

	public void setServerInfo(ModelProto.ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}

	public RoomClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<RoomClient>) client -> {

			ServerType serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				Room.getInstance().serverClientManager.removeServerClient(serverType, serverInfo.getServerId());
			}
		});
	}
}
