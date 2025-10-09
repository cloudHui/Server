package robot.connect.handle.center;

import java.util.UUID;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import msg.registor.enums.ServerType;
import msg.registor.message.HMsg;
import proto.HallProto;
import proto.ModelProto;
import robot.Robot;
import robot.connect.ConnectProcessor;
import robot.connect.handle.RobotHandle;

/**
 * gate信息回复
 */
@ProcessClass(ModelProto.AckServerInfo.class)
public class AckServerInfoHandle implements RobotHandle {

	@Override
	public void handle(Message message) {
		if (message instanceof ModelProto.AckServerInfo) {
			ModelProto.AckServerInfo ack = (ModelProto.AckServerInfo) message;
			LOGGER.error("handle:{}", ack.getServers(0).getServerType() +
					" " + ack.getServers(0).getServerId() +
					" " + ack.getServers(0).getIpConfig().toStringUtf8());
			Robot.getInstance().execute(() -> Robot.getInstance().getServerManager().connectToSever(
					ack.getServersList(),
					Robot.getInstance().getServerId(),
					Robot.getInstance().getInnerIp() + ":" + Robot.getInstance().getPort(),
					ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
					ConnectProcessor.HANDLERS, ServerType.Robot));

			Robot.getInstance().getClientSendMessage(ServerType.Hall, HMsg.REQ_LOGIN_MSG, HallProto.ReqLogin.newBuilder()
					.setNickName(ByteString.copyFromUtf8(UUID.randomUUID().toString()))
					.setCert(ByteString.copyFromUtf8(Robot.getInstance().getInnerIp()))
					.build());
		}
	}
}
