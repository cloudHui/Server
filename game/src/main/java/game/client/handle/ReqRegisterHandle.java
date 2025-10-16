package game.client.handle;

import com.google.protobuf.Message;
import game.Game;
import game.client.GameClient;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

/**
 * 处理网关服务器注册请求
 * 负责管理网关服务器到游戏服务器的连接注册
 */
@ProcessType(CMsg.REQ_REGISTER)
public class ReqRegisterHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqRegisterHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, int mapId, int sequence) {
		try {
			ModelProto.ReqRegister request = (ModelProto.ReqRegister) message;
			ModelProto.ServerInfo serverInfo = request.getServerInfo();
			ServerType serverType = ServerType.get(serverInfo.getServerType());

			if (serverType == null) {
				logger.error("未知的服务器类型: {}", serverInfo.getServerType());
				return true;
			}

			logger.info("处理网关注册请求, serverType: {}, serverId: {}, address: {}",
					serverType, serverInfo.getServerId(), serverInfo.getIpConfig().toStringUtf8());

			// 处理网关注册
			processGatewayRegistration(sender, serverInfo, serverType);

			// 发送注册响应
			sendRegistrationResponse(sender, clientId, mapId, sequence);

			return true;
		} catch (Exception e) {
			logger.error("处理网关注册请求失败", e);
			return false;
		}
	}

	/**
	 * 处理网关注册
	 */
	private void processGatewayRegistration(Sender sender, ModelProto.ServerInfo serverInfo, ServerType serverType) {
		GameClient client = (GameClient) sender;
		client.setServerInfo(serverInfo);

		Game.getInstance().getServerClientManager().addServerClient(serverType, client, serverInfo.getServerId());

		logger.info("网关注册成功, serverType: {}, serverId: {}", serverType, serverInfo.getServerId());
	}

	/**
	 * 发送注册响应
	 */
	private void sendRegistrationResponse(Sender sender, int clientId, int mapId, int sequence) {
		ModelProto.AckRegister.Builder response = ModelProto.AckRegister.newBuilder();
		response.setServerInfo(Game.getInstance().getServerInfo().build());

		sender.sendMessage(clientId, CMsg.ACK_REGISTER, mapId, response.build(), sequence);
		logger.debug("已发送注册响应, clientId: {}", clientId);
	}
}