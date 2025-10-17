package room.client.handle.server.hall;

import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import proto.ModelProto;
import room.Room;
import room.client.RoomClient;
import utils.handle.AbstractRegisterHandler;

/**
 * 注册服务信息请求
 */
@ProcessType(CMsg.REQ_REGISTER)
public class ReqRegisterHandler extends AbstractRegisterHandler<RoomClient, Room> {

	@Override
	protected Room getServerInstance() {
		return Room.getInstance();
	}

	@Override
	protected RoomClient castSender(Sender sender) {
		return (RoomClient) sender;
	}

	@Override
	protected void addServerClient(ServerType serverType, RoomClient client, int serverId) {
		getServerInstance().getServerClientManager().addServerClient(serverType, client, serverId);
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
		RoomClient client = castSender(sender);
		client.setServerInfo(serverInfo);
	}
}