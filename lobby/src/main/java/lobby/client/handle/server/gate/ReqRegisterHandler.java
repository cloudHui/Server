package lobby.client.handle.server.gate;

import lobby.Lobby;
import lobby.client.LobbyClient;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import proto.ModelProto;
import tools.handle.AbstractRegisterHandler;

@ProcessType(CMsg.REQ_REGISTER)
public class ReqRegisterHandler extends AbstractRegisterHandler<Lobby> {

	@Override
	protected Lobby getServerInstance() {
		return Lobby.getInstance();
	}

	@Override
	protected void addServerClient(ServerType serverType, Sender client) {
		getServerInstance().serverClientManager.addServerClient(serverType, (LobbyClient) client);
	}

	@Override
	protected ModelProto.ServerInfo getCurrentServerInfo() {
		return getServerInstance().getServerInfo();
	}

	@Override
	protected void beforeRegistration(Sender sender, ModelProto.ServerInfo serverInfo, ServerType serverType) {
		((LobbyClient) sender).setServerInfo(serverInfo);
	}
}
