package room.connect.center.time;

import java.util.Collections;
import java.util.List;

import msg.annotation.ProcessClass;
import msg.registor.enums.ServerType;
import net.connect.handle.ConnectHandler;
import proto.ModelProto;
import proto.ServerProto;
import room.Room;
import room.connect.ConnectProcessor;
import utils.handle.AbstractAckServerInfoHandle;

@ProcessClass(ServerProto.AckServerInfo.class)
public class AckServerInfoHandle extends AbstractAckServerInfoHandle {

	private static final List<ServerType> RETRY_SERVER_TYPES = Collections.singletonList(ServerType.Game);

	@Override
	protected void processServerInfo(ServerProto.AckServerInfo response) {
		ModelProto.ServerInfo serverInfo = response.getServers(0);
		logger.info("处理请求需要连接的服务器信息返回, response:{}", response.toString());

		Room.getInstance().execute(() ->
				Room.getInstance().getServerManager().connectToSingleServer(
						serverInfo,
						Room.getInstance().getServerId(),
						Room.getInstance().getInnerIp() + ":" + Room.getInstance().getPort(),
						ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
						ConnectProcessor.HANDLERS, ServerType.Room, null));
	}

	@Override
	protected void scheduleRetry(ConnectHandler serverClient) {
		logger.error("未找到可用服务器,将在 {}ms 后重试", RETRY_DELAY);

		Room.getInstance().registerTimer(RETRY_DELAY, RETRY_INTERVAL, RETRY_COUNT, room -> {
			sendRetryRequest(serverClient, RETRY_SERVER_TYPES, ConnectProcessor.PARSER);
			return true;
		}, Room.getInstance());
	}
}