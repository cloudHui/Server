package gate.client.handle.back;

import com.google.protobuf.ByteString;
import gate.Gate;
import gate.client.GateTcpClient;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import msg.registor.message.HMsg;
import net.client.handler.ClientHandler;
import net.connect.handle.ConnectHandler;
import net.message.TCPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import proto.ModelProto;

/**
 * @author admin
 * @className LoginBack
 * @description
 * @createDate 2025/10/21 15:41
 */
@ProcessType(HMsg.ACK_LOGIN_MSG)
public class LoginBack implements BackHandle {

	private static final Logger logger = LoggerFactory.getLogger(LoginBack.class);

	@Override
	public void handle(TCPMessage response, GateTcpClient client) {
		try {
			HallProto.AckLogin res = HallProto.AckLogin.parseFrom(response.getMessage());
			client.setRoleId(res.getUserId());
			client.setClubId(res.getClub());
			client.setChannel(res.getChannel());

			notifyCenterLoginSuccess(ClientHandler.getRemoteIP(client).getHostString());
			logger.info("用户登录成功, userId: {}, channel: {}, club: {}", res.getUserId(), res.getChannel(), res.getClub());
		} catch (Exception e) {
			logger.error("解析登录响应失败, msgId: {}, userId: {}", Integer.toHexString(response.getMessageId()), client.getRoleId(), e);
		}
	}

	/**
	 * 通知中心服务器登录成功
	 */
	private void notifyCenterLoginSuccess(String certificate) {
		ModelProto.NotRegisterClient.Builder loginNotify = ModelProto.NotRegisterClient.newBuilder();
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
