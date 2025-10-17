package robot.connect.handle.center;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.protobuf.ByteString;
import msg.annotation.ProcessClass;
import msg.registor.enums.ServerType;
import msg.registor.message.HMsg;
import net.connect.TCPConnect;
import net.connect.handle.ConnectHandler;
import proto.HallProto;
import proto.ModelProto;
import robot.Robot;
import robot.connect.ConnectProcessor;
import utils.handle.AbstractAckServerInfoHandle;
import utils.manager.HandleManager;

@ProcessClass(ModelProto.AckServerInfo.class)
public class AckServerInfoHandle extends AbstractAckServerInfoHandle {

	private static final List<ServerType> RETRY_SERVER_TYPES = Collections.singletonList(ServerType.Gate);

	@Override
	protected void processServerInfo(ModelProto.AckServerInfo response) {
		ModelProto.ServerInfo serverInfo = response.getServers(0);
		logger.info("处理请求需要连接的服务器信息返回, response:{}", response.toString());

		Robot.getInstance().execute(() -> Robot.getInstance().getServerManager().connectToSingleServer(
				serverInfo,
				Robot.getInstance().getServerId(),
				Robot.getInstance().getInnerIp() + ":" + Robot.getInstance().getPort(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				//Todo 这里后面测多个机器人的时候需要改成登录多个机器人
				ConnectProcessor.HANDLERS, ServerType.Robot, new TCPConnect.CallParam(HMsg.REQ_LOGIN_MSG, HallProto.ReqLogin.newBuilder()
						.setNickName(ByteString.copyFromUtf8(UUID.randomUUID().toString()))
						.setCert(ByteString.copyFromUtf8(Robot.getId()))
						.build(), HandleManager::sendMsg, ConnectProcessor.PARSER)));
	}

	@Override
	protected void scheduleRetry(ConnectHandler serverClient) {
		logger.error("未找到可用服务器，将在 {}ms 后重试", RETRY_DELAY);

		Robot.getInstance().registerTimer(RETRY_DELAY, RETRY_INTERVAL, RETRY_COUNT, room -> {
			sendRetryRequest(serverClient, RETRY_SERVER_TYPES, ConnectProcessor.PARSER);
			return true;
		}, Robot.getInstance());
	}
}