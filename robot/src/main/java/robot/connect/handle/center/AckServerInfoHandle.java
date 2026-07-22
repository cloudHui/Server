package robot.connect.handle.center;

import java.util.Collections;
import java.util.List;

import com.google.protobuf.ByteString;
import msg.annotation.ProcessClass;
import msg.registor.enums.ServerType;
import msg.registor.message.LMsg;
import net.connect.TCPConnect;
import net.client.Sender;
import proto.LobbyProto;
import proto.ModelProto;
import proto.ServerProto;
import robot.Robot;
import robot.connect.ConnectProcessor;
import tools.handle.AbstractAckServerInfoHandle;
import tools.manager.HandleManager;

@ProcessClass(ServerProto.AckServerInfo.class)
public class AckServerInfoHandle extends AbstractAckServerInfoHandle {

	private static final List<ServerType> RETRY_SERVER_TYPES = Collections.singletonList(ServerType.Gate);

	@Override
	protected void processServerInfo(ServerProto.AckServerInfo response) {
		if (response.getServersList().isEmpty()) {
			logger.warn("服务器信息列表为空, response:{}", response.toString());
			return;
		}
		ModelProto.ServerInfo serverInfo = response.getServers(0);
		logger.info("处理请求需要连接的服务器信息返回, response:{}", response.toString());

		Robot.getInstance().execute(() -> Robot.getInstance().getServerManager().connectToSingleServer(
				serverInfo,
				Robot.getInstance().getServerId(),
				Robot.getInstance().getInnerIp() + ":" + Robot.getInstance().getPort(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Robot, new TCPConnect.CallParam(LMsg.REQ_LOGIN_MSG,
						LobbyProto.ReqLogin.newBuilder()
								.setUsername(ByteString.copyFromUtf8("admin"))
								.setPassword(ByteString.copyFromUtf8("admin123"))
								.build(),
						HandleManager::sendMsg, ConnectProcessor.PARSER)));
	}

	@Override
	protected void scheduleRetry(Sender serverClient) {
		logger.error("未找到可用服务器,将在 {}ms 后重试", RETRY_DELAY);

		Robot.getInstance().registerTimer(RETRY_DELAY, RETRY_INTERVAL, RETRY_COUNT, room -> {
			sendRetryRequest(serverClient, RETRY_SERVER_TYPES, ConnectProcessor.PARSER);
			return true;
		}, Robot.getInstance());
	}
}
