package game.client;

import game.Game;
import msg.registor.enums.ServerType;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import proto.ModelProto;


public class GameClient extends ClientHandler {

	private ModelProto.ServerInfo serverInfo;

	public GameClient() {
		super(ClientProto.PARSER, ClientProto.GET, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent(client -> {
			ServerType serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				Game.getInstance().getServerClientManager().removeServerClient(serverType, serverInfo.getServerId());
			}
		});
	}

	public void setServerInfo(ModelProto.ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}
}
