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

@ProcessType(LMsg.ACK_LOGIN_MSG)
public class LoginBack implements BackHandle, Handler {

	private static final Logger logger = LoggerFactory.getLogger(LoginBack.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, long mapId, int sequence) {
		return false;
	}

	@Override
	public void handle(TCPMessage response, GateTcpClient client) {
		try {
			LobbyProto.AckLogin res = LobbyProto.AckLogin.parseFrom(response.getMessage());
			if (res.getCode() == 0 && res.getUserId() > 0) {
				client.setRoleId(res.getUserId());
				notifyCenterLoginSuccess(ClientHandler.getRemoteIP(client).getHostString());
				logger.info("用户登录成功, userId: {}", res.getUserId());
			} else {
				logger.warn("登录失败, code: {}, userId: {}", res.getCode(), res.getUserId());
			}
		} catch (Exception e) {
			logger.error("解析登录响应失败, msgId: {}, userId: {}",
					Integer.toHexString(response.getMessageId()), client.getRoleId(), e);
		}
	}

	private void notifyCenterLoginSuccess(String certificate) {
		ServerProto.NotRegisterClient.Builder loginNotify = ServerProto.NotRegisterClient.newBuilder();
		loginNotify.setCert(ByteString.copyFromUtf8(certificate));

		ConnectHandler centerConnection = Gate.getInstance().getServerManager().getServerClient(ServerType.Center);
		if (centerConnection != null) {
			centerConnection.sendMessage(CMsg.NOT_LINK, loginNotify.build());
			logger.debug("已通知中心服务器登录成功, certificate: {}", certificate);
		} else {
			logger.warn("中心服务器连接不可用,无法通知登录成功");
		}
	}
}
