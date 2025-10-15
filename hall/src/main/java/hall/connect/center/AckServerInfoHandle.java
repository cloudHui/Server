package hall.connect.center;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.Message;
import hall.Hall;
import hall.connect.ConnectProcessor;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.connect.handle.ConnectHandler;
import proto.ModelProto;
import utils.manager.ConnectHandle;
import utils.manager.HandleManager;

/**
 * 处理中心服务器返回的服务信息响应
 * 负责连接到大堂需要的其他服务
 */
@ProcessType(CMsg.ACK_SERVER)
public class AckServerInfoHandle implements ConnectHandle {
	private static final Logger logger = LoggerFactory.getLogger(AckServerInfoHandle.class);

	// 重试配置
	private static final long RETRY_DELAY = 5000;
	private static final long RETRY_INTERVAL = 1000;
	private static final int RETRY_COUNT = 1;

	@Override
	public void handle(Message message, ConnectHandler serverClient, int sequence) {
		try {
			if (message instanceof ModelProto.AckServerInfo) {
				ModelProto.AckServerInfo response = (ModelProto.AckServerInfo) message;

				if (response.getServersCount() > 0) {
					processServerInfo(response);
				} else {
					scheduleRetry(serverClient);
				}
			}
		} catch (Exception e) {
			logger.error("处理服务信息响应失败", e);
		}
	}

	/**
	 * 处理服务器信息
	 */
	private void processServerInfo(ModelProto.AckServerInfo response) {
		ModelProto.ServerInfo serverInfo = response.getServers(0);

		logger.info("处理服务器信息, serverType: {}, serverId: {}, address: {}",
				serverInfo.getServerType(), serverInfo.getServerId(),
				serverInfo.getIpConfig().toStringUtf8());

		Hall.getInstance().execute(() ->
				Hall.getInstance().getServerManager().connectToSingleServer(
						serverInfo,
						Hall.getInstance().getServerId(),
						Hall.getInstance().getInnerIp() + ":" + Hall.getInstance().getPort(),
						ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
						ConnectProcessor.HANDLERS, ServerType.Hall, null));
	}

	/**
	 * 调度重试机制
	 */
	private void scheduleRetry(ConnectHandler serverClient) {
		logger.warn("未找到可用服务器，将在 {}ms 后重试", RETRY_DELAY);

		Hall.getInstance().registerTimer(RETRY_DELAY, RETRY_INTERVAL, RETRY_COUNT, robot -> {
			ModelProto.ReqServerInfo request = ModelProto.ReqServerInfo.newBuilder()
					.addServerType(ServerType.Room.getServerType())
					.build();

			HandleManager.sendMsg(CMsg.REQ_SERVER, request, serverClient, ConnectProcessor.PARSER);
			return true;
		}, Hall.getInstance());
	}
}