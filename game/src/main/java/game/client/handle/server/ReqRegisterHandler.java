package game.client.handle.server;

import game.Game;
import game.client.GameClient;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import proto.ModelProto;
import utils.handle.AbstractRegisterHandler;

/**
 * 注册服务信息请求
 */
@ProcessType(CMsg.REQ_REGISTER)
public class ReqRegisterHandler extends AbstractRegisterHandler<Game> {

	@Override
	protected Game getServerInstance() {
		return Game.getInstance();
	}

	@Override
	protected void addServerClient(ServerType serverType, Sender client) {
		getServerInstance().getServerClientManager().addServerClient(serverType, (GameClient) client);
	}

	@Override
	protected ModelProto.ServerInfo getCurrentServerInfo() {
		return getServerInstance().getServerInfo();
	}

	/**
	 * 注册前处理：设置服务器信息
	 */
	@Override
	protected void beforeRegistration(Sender sender, ModelProto.ServerInfo serverInfo, ServerType serverType) {
		((GameClient) sender).setServerInfo(serverInfo);
	}
}