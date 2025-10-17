package gate.connect.handle.time;

import java.util.Arrays;
import java.util.List;

import gate.Gate;
import gate.connect.ConnectProcessor;
import msg.annotation.ProcessClass;
import msg.registor.enums.ServerType;
import net.connect.handle.ConnectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.handle.AbstractAckServerInfoHandle;

@ProcessClass(ModelProto.AckServerInfo.class)
public class AckServerInfoHandle extends AbstractAckServerInfoHandle {
	private static final Logger logger = LoggerFactory.getLogger(AckServerInfoHandle.class);

	private static final List<ServerType> RETRY_SERVER_TYPES = Arrays.asList(ServerType.Room, ServerType.Game, ServerType.Hall);

	@Override
	protected void processServerInfo(ModelProto.AckServerInfo response) {
		logger.info("处理请求需要连接的服务器信息返回, response:{}", response.toString());

		Gate.getInstance().execute(() ->
				Gate.getInstance().getServerManager().connectToSever(
						response.getServersList(),
						Gate.getInstance().getServerId(),
						Gate.getInstance().getInnerIp() + ":" + Gate.getInstance().getPort(),
						null, ConnectProcessor.PARSER,
						ConnectProcessor.HANDLERS, ServerType.Gate));
	}

	@Override
	protected void scheduleRetry(ConnectHandler serverClient) {
		logger.warn("未找到可用服务器，将在 {}ms 后重试", RETRY_DELAY);

		Gate.getInstance().registerTimer(RETRY_DELAY, RETRY_INTERVAL, RETRY_COUNT, gate -> {
			sendRetryRequest(serverClient, RETRY_SERVER_TYPES, ConnectProcessor.PARSER);
			return true;
		}, Gate.getInstance());
	}
}