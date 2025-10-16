package room.client.handle.server.center;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import room.Room;
import room.client.RoomClient;

/**
 * 处理服务注册请求
 * 只有网关服务器会主动连接房间服务器进行注册
 */
@ProcessType(CMsg.REQ_REGISTER)
public class ReqRegisterHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqRegisterHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, int sequence) {
		try {
			ModelProto.ReqRegister request = (ModelProto.ReqRegister) msg;
			ModelProto.ServerInfo serverInfo = request.getServerInfo();
			ServerType serverType = ServerType.get(serverInfo.getServerType());

			if (serverType == null) {
				logger.error("未知的服务器类型: {}", serverInfo.getServerType());
				return true;
			}

			RoomClient client = (RoomClient) sender;
			processServerRegistration(client, serverInfo, serverType);

			sendRegistrationResponse(sender, clientId, mapId, sequence);

			logger.info("服务器注册成功, serverType: {}, serverId: {}, address: {}",
					serverType, serverInfo.getServerId(), serverInfo.getIpConfig().toStringUtf8());
			return true;
		} catch (Exception e) {
			logger.error("处理服务注册请求失败", e);
			return false;
		}
	}

	/**
	 * 处理服务器注册
	 */
	private void processServerRegistration(RoomClient client, ModelProto.ServerInfo serverInfo, ServerType serverType) {
		client.setServerInfo(serverInfo);
		Room.getInstance().getServerClientManager().addServerClient(serverType, client, serverInfo.getServerId());
		logger.debug("已添加服务器客户端, serverType: {}, serverId: {}", serverType, serverInfo.getServerId());
	}

	/**
	 * 发送注册响应
	 */
	private void sendRegistrationResponse(Sender sender, int clientId, int mapId, int sequence) {
		ModelProto.AckRegister.Builder response = ModelProto.AckRegister.newBuilder();
		response.setServerInfo(Room.getInstance().getServerInfo().build());

		sender.sendMessage(clientId, CMsg.ACK_REGISTER, mapId, response.build(), sequence);
		logger.debug("已发送注册响应, clientId: {}", clientId);
	}
}