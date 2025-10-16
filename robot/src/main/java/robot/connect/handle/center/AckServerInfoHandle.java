package robot.connect.handle.center;

import java.util.UUID;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import msg.registor.message.HMsg;
import net.connect.TCPConnect;
import net.connect.handle.ConnectHandler;
import proto.HallProto;
import proto.ModelProto;
import robot.Robot;
import robot.connect.ConnectProcessor;
import utils.manager.ConnectHandle;
import utils.manager.HandleManager;

/**
 * gate信息回复
 */
@ProcessClass(ModelProto.AckServerInfo.class)
public class AckServerInfoHandle implements ConnectHandle {

	@Override
	public void handle(Message message, ConnectHandler serverClient, int sequence) {
		if (message instanceof ModelProto.AckServerInfo) {
			ModelProto.AckServerInfo ack = (ModelProto.AckServerInfo) message;
			if (ack.getServersCount() > 0) {
				LOGGER.error("handle:{}", ack.getServers(0).getServerType() +
						" " + ack.getServers(0).getServerId() +
						" " + ack.getServers(0).getIpConfig().toStringUtf8());
				Robot.getInstance().execute(() -> Robot.getInstance().getServerManager().connectToSingleServer(
						ack.getServersList().get(0),
						Robot.getInstance().getServerId(),
						Robot.getInstance().getInnerIp() + ":" + Robot.getInstance().getPort(),
						ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
						//Todo 这里后面测多个机器人的时候需要改成登录多个机器人
						ConnectProcessor.HANDLERS, ServerType.Robot, new TCPConnect.CallParam(HMsg.REQ_LOGIN_MSG, HallProto.ReqLogin.newBuilder()
								.setNickName(ByteString.copyFromUtf8(UUID.randomUUID().toString()))
								.setCert(ByteString.copyFromUtf8(Robot.getId()))
								.build(), HandleManager::sendMsg, ConnectProcessor.PARSER)));
			} else {
				//加入五秒重试机制
				Robot.getInstance().registerTimer(5000, 1000, 1, robot -> {
					HandleManager.sendMsg(CMsg.REQ_SERVER, ModelProto.ReqServerInfo.newBuilder()
							.addServerType(ServerType.Gate.getServerType())
							.build(), serverClient, ConnectProcessor.PARSER);
					return true;
				}, Robot.getInstance());
			}
		}
	}
}
