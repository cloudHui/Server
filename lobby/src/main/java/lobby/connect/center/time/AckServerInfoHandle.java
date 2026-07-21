package lobby.connect.center.time;

import java.util.Collections;
import java.util.List;

import lobby.Lobby;
import lobby.connect.ConnectProcessor;
import msg.annotation.ProcessClass;
import msg.registor.enums.ServerType;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.connect.TCPConnect;
import proto.ModelProto;
import proto.ServerProto;
import utils.handle.AbstractAckServerInfoHandle;
import utils.manager.HandleManager;

@ProcessClass(ServerProto.AckServerInfo.class)
public class AckServerInfoHandle extends AbstractAckServerInfoHandle {

	private static final List<ServerType> RETRY_SERVER_TYPES = Collections.singletonList(ServerType.Game);

	@Override
	protected void processServerInfo(ServerProto.AckServerInfo response) {
		if (response.getServersCount() == 0) {
			logger.error("服务器信息列表为空,无法处理");
			return;
		}
		ModelProto.ServerInfo serverInfo = response.getServers(0);
		logger.info("处理请求需要连接的服务器信息返回, response:{}", response.toString());

		ServerProto.ReqRoomTables reqRoomTables = ServerProto.ReqRoomTables.newBuilder()
				.setRoomServerId(Lobby.getInstance().getServerId())
				.build();
		TCPConnect.CallParam callParam = new TCPConnect.CallParam(
				SMsg.REQ_ROOM_TABLES_MSG, reqRoomTables,
				HandleManager::sendMsg, ConnectProcessor.PARSER);

		Lobby.getInstance().execute(() ->
				Lobby.getInstance().getServerManager().connectToSingleServer(
						serverInfo,
						Lobby.getInstance().getServerId(),
						Lobby.getInstance().getInnerIp() + ":" + Lobby.getInstance().getPort(),
						ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
						ConnectProcessor.HANDLERS, ServerType.Lobby, callParam));
	}

	@Override
	protected void scheduleRetry(Sender serverClient) {
		logger.error("未找到可用服务器,将在 {}ms 后重试", RETRY_DELAY);
		Lobby.getInstance().registerTimer(RETRY_DELAY, RETRY_INTERVAL, RETRY_COUNT, lobby -> {
			sendRetryRequest(serverClient, RETRY_SERVER_TYPES, ConnectProcessor.PARSER);
			return true;
		}, Lobby.getInstance());
	}
}
