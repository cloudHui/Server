package hall.client.handle.server.gate;

import hall.Hall;
import hall.client.HallClient;
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
public class ReqRegisterHandler extends AbstractRegisterHandler<Hall> {

	@Override
	protected Hall getServerInstance() {
		return Hall.getInstance();
	}

	@Override
	protected void addServerClient(ServerType serverType, Sender client) {
		getServerInstance().serverClientManager.addServerClient(serverType, (HallClient) client);
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
		((HallClient) sender).setServerInfo(serverInfo);
	}
}