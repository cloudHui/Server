package gate.client.handle.back;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import gate.Gate;
import gate.client.GateTcpClient;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import msg.registor.message.LMsg;
import net.client.Sender;
import net.client.handler.ClientHandler;
import net.connect.handle.ConnectHandler;
import net.handler.Handler;
import net.message.TCPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.LobbyProto;
import proto.ServerProto;

@ProcessType(LMsg.ACK_REGISTER_MSG)
public class RegisterBack implements BackHandle, Handler {

	private static final Logger logger = LoggerFactory.getLogger(RegisterBack.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, long mapId, int sequence) {
		return false;
	}

	@Override
	public void handle(TCPMessage response, GateTcpClient client) {
		try {
			LobbyProto.AckUserRegister res = LobbyProto.AckUserRegister.parseFrom(response.getMessage());
			if (res.getCode() == 0 && res.getUserId() > 0) {
				client.setRoleId(res.getUserId());
				notifyCenterLoginSuccess(ClientHandler.getRemoteIP(client).getHostString());
				logger.info("用户注册成功, userId: {}", res.getUserId());
			} else {
				logger.warn("注册失败, code: {}, userId: {}", res.getCode(), res.getUserId());
			}
		} catch (Exception e) {
			logger.error("解析注册响应失败, msgId: {}", Integer.toHexString(response.getMessageId()), e);
		}
	}

	private void notifyCenterLoginSuccess(String certificate) {
		ServerProto.NotRegisterClient.Builder loginNotify = ServerProto.NotRegisterClient.newBuilder();
		loginNotify.setCert(ByteString.copyFromUtf8(certificate));
		ConnectHandler centerConnection = Gate.getInstance().getServerManager().getServerClient(ServerType.Center);
		if (centerConnection != null) {
			centerConnection.sendMessage(CMsg.NOT_LINK, loginNotify.build());
		}
	}
}
