package hall.connect.center.time;

import java.util.Collections;
import java.util.List;

import hall.Hall;
import hall.connect.ConnectProcessor;
import msg.annotation.ProcessClass;
import msg.registor.enums.ServerType;
import net.connect.handle.ConnectHandler;
import proto.ModelProto;
import proto.ServerProto;
import utils.handle.AbstractAckServerInfoHandle;

@ProcessClass(ServerProto.AckServerInfo.class)
public class AckServerInfoHandle extends AbstractAckServerInfoHandle {

	private static final List<ServerType> RETRY_SERVER_TYPES = Collections.singletonList(ServerType.Room);

	@Override
	protected void processServerInfo(ServerProto.AckServerInfo response) {
		ModelProto.ServerInfo serverInfo = response.getServers(0);
		logger.info("处理请求需要连接的服务器信息返回, response:{}", response.toString());

		Hall.getInstance().execute(() ->
				Hall.getInstance().getServerManager().connectToSingleServer(
						serverInfo,
						Hall.getInstance().getServerId(),
						Hall.getInstance().getInnerIp() + ":" + Hall.getInstance().getPort(),
						ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
						ConnectProcessor.HANDLERS, ServerType.Hall, null));
	}

	@Override
	protected void scheduleRetry(ConnectHandler serverClient) {
		logger.warn("未找到可用服务器,将在 {}ms 后重试", RETRY_DELAY);

		Hall.getInstance().registerTimer(RETRY_DELAY, RETRY_INTERVAL, RETRY_COUNT, hall -> {
			sendRetryRequest(serverClient, RETRY_SERVER_TYPES, ConnectProcessor.PARSER);
			return true;
		}, Hall.getInstance());
	}
}