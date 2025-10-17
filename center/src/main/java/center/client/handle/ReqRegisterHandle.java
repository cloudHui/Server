package center.client.handle;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import center.Center;
import center.client.CenterClient;
import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.client.handler.ClientHandler;
import net.handler.Handler;
import proto.ModelProto;
import utils.ServerClientManager;

/**
 * 处理服务注册请求
 * 负责管理所有服务器的注册和发现
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

			logger.info("处理服务注册请求, serverType: {}, serverId: {}, address: {}",
					serverType, serverInfo.getServerId(), serverInfo.getIpConfig().toStringUtf8());

			// 处理服务器注册
			processServerRegistration(sender, serverInfo, serverType);

			// 发送注册响应
			sendRegistrationResponse(sender, serverType, sequence);

			// 通知相关服务器新服务上线
			notifyRelatedServers(serverInfo, serverType);

			return true;
		} catch (Exception e) {
			logger.error("处理服务注册请求失败", e);
			return false;
		}
	}

	/**
	 * 处理服务器注册
	 */
	private void processServerRegistration(Sender sender, ModelProto.ServerInfo serverInfo, ServerType serverType) {
		ServerClientManager manager = Center.getInstance().getServerManager();
		int serverId = serverInfo.getServerId();

		// 检查是否已存在相同服务器，如果存在则先移除
		CenterClient existingClient = (CenterClient) manager.getServerClient(serverType, serverId);
		if (existingClient != null) {
			logger.warn("服务器已存在，先移除旧连接, serverType: {}, serverId: {}", serverType, serverId);
			manager.removeServerClient(serverType, serverId);
			// 设置旧连接为不安全以触发关闭
			existingClient.setSafe((msgId) -> false);
		}

		// 注册新服务器
		CenterClient newClient = (CenterClient) sender;
		newClient.setServerInfo(serverInfo);
		manager.addServerClient(serverType, newClient, serverId);

		logger.info("服务器注册成功, serverType: {}, serverId: {}", serverType, serverId);
	}

	/**
	 * 发送注册响应
	 */
	private void sendRegistrationResponse(Sender sender, ServerType serverType, int sequence) {
		ModelProto.AckRegister.Builder response = ModelProto.AckRegister.newBuilder();
		response.setServerInfo(Center.getInstance().getServerInfo().build());

		sender.sendMessage(CMsg.ACK_REGISTER, response.build(), sequence);
		logger.info("已发送注册响应, serverType: {}", serverType);
	}

	/**
	 * 通知相关服务器新服务上线
	 */
	private void notifyRelatedServers(ModelProto.ServerInfo serverInfo, ServerType serverType) {
		ServerClientManager manager = Center.getInstance().getServerManager();

		switch (serverType) {
			case Game:
				// 游戏服务器上线，通知网关和房间服务器
				notifyServerConnect(manager, serverInfo, ServerType.Gate);
				notifyServerConnect(manager, serverInfo, ServerType.Room);
				break;
			case Room:
				// 房间服务器上线，通知网关和大厅服务器
				notifyServerConnect(manager, serverInfo, ServerType.Gate);
				notifyServerConnect(manager, serverInfo, ServerType.Hall);
				break;
			case Hall:
				// 大厅服务器上线，通知网关
				notifyServerConnect(manager, serverInfo, ServerType.Gate);
				break;
			case Gate:
				// 网关服务器上线，不需要特别通知其他服务器
				logger.info("网关服务器注册，无需特别通知");
				break;
			default:
				logger.error("未知服务器类型，不进行通知: {}", serverType);
				break;
		}
	}

	/**
	 * 通知指定类型的服务器有新服务连接
	 */
	private void notifyServerConnect(ServerClientManager manager, ModelProto.ServerInfo serverInfo, ServerType targetServerType) {
		List<ClientHandler> targetServers = manager.getAllTypeServer(targetServerType);
		if (targetServers == null || targetServers.isEmpty()) {
			logger.info("目标服务器类型暂无在线实例: {}", targetServerType);
			return;
		}

		ModelProto.NotRegisterInfo.Builder notification = ModelProto.NotRegisterInfo.newBuilder();
		notification.addServers(serverInfo);

		int notifiedCount = 0;
		for (ClientHandler client : targetServers) {
			try {
				client.sendMessage(CMsg.REGISTER_NOTICE, notification.build());
				notifiedCount++;
			} catch (Exception e) {
				logger.debug("通知服务器失败, targetType: {}, serverId: {}",
						targetServerType, ((CenterClient) client).getServerInfo().getServerId(), e);
			}
		}

		logger.info("已通知服务器新服务上线, targetType: {}, 通知数量: {}/{}, 新服务: {}-{}",
				targetServerType, notifiedCount, targetServers.size(),
				serverInfo.getServerType(), serverInfo.getServerId());
	}
}