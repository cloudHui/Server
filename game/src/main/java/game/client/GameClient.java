package game.client;

import game.Game;
import msg.ServerType;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;
import proto.ModelProto;


public class GameClient extends ClientHandler {

	private ModelProto.ServerInfo serverInfo;

	public GameClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent( client -> {
			ServerType serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				Game.getInstance().serverClientManager.removeServerClient(serverType, serverInfo.getServerId());
			}
		});
	}

	public void setServerInfo(ModelProto.ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}
}
