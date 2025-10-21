package center.client.handle;

import java.util.List;

import center.Center;
import center.client.CenterClient;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.client.handler.ClientHandler;
import proto.ModelProto;
import utils.ServerClientManager;
import utils.handle.AbstractRegisterHandler;

/**
 * 处理服务注册请求
 * 负责管理所有服务器的注册和发现
 */
@ProcessType(CMsg.REQ_REGISTER)
public class ReqRegisterHandle extends AbstractRegisterHandler<Center> {
	private ServerClientManager manager;

	@Override
	protected Center getServerInstance() {
		return Center.getInstance();
	}

	@Override
	protected void addServerClient(ServerType serverType, Sender client) {
		if (manager == null) {
			manager = getServerInstance().getServerManager();
		}
		manager.addServerClient(serverType, (CenterClient) client);
	}

	@Override
	protected ModelProto.ServerInfo getCurrentServerInfo() {
		return getServerInstance().getServerInfo();
	}

	/**
	 * 注册前处理：检查并移除重复连接,设置服务器信息
	 */
	@Override
	protected void beforeRegistration(Sender sender, ModelProto.ServerInfo serverInfo, ServerType serverType) {
		if (manager == null) {
			manager = getServerInstance().getServerManager();
		}

		int serverId = serverInfo.getServerId();
		CenterClient existingClient = (CenterClient) manager.getServerClient(serverType, serverId);

		if (existingClient != null) {
			logger.warn("服务器已存在,先移除旧连接, serverType: {}, serverId: {}", serverType, serverId);
			manager.removeServerClient(serverType, serverId);
			// 设置旧连接为不安全以触发关闭
			existingClient.setSafe((msgId) -> false);
		}

		// 设置新连接的服务器信息
		((CenterClient) sender).setServerInfo(serverInfo);
	}

	/**
	 * 注册后处理：通知相关服务器
	 */
	@Override
	protected void afterRegistration(Sender sender, ModelProto.ServerInfo serverInfo, ServerType serverType) {
		notifyRelatedServers(serverInfo, serverType);
	}

	/**
	 * 重写发送响应方法（Center服务器使用不同的参数）
	 */
	@Override
	protected void sendRegistrationResponse(Sender sender, int clientId, long mapId, int sequence) {
		ModelProto.AckRegister.Builder response = ModelProto.AckRegister.newBuilder();
		response.setServerInfo(getCurrentServerInfo());
		sender.sendMessage(CMsg.ACK_REGISTER, response.build(), sequence);
		logger.info("已发送注册响应");
	}

	/**
	 * 通知相关服务器新服务上线
	 */
	private void notifyRelatedServers(ModelProto.ServerInfo serverInfo, ServerType serverType) {
		if (manager == null) {
			manager = getServerInstance().getServerManager();
		}

		switch (serverType) {
			case Game:
				// 游戏服务器上线,通知网关和房间服务器
				notifyServerConnect(serverInfo, ServerType.Gate);
				notifyServerConnect(serverInfo, ServerType.Room);
				break;
			case Room:
				// 房间服务器上线,通知网关和大厅服务器
				notifyServerConnect(serverInfo, ServerType.Gate);
				notifyServerConnect(serverInfo, ServerType.Hall);
				break;
			case Hall:
				// 大厅服务器上线,通知网关
				notifyServerConnect(serverInfo, ServerType.Gate);
				break;
			case Gate:
				// 网关服务器上线,不需要特别通知其他服务器
				logger.info("网关服务器注册,无需特别通知");
				break;
			default:
				logger.error("未知服务器类型,不进行通知: {}", serverType);
				break;
		}
	}

	/**
	 * 通知指定类型的服务器有新服务连接
	 */
	private void notifyServerConnect(ModelProto.ServerInfo serverInfo, ServerType targetServerType) {
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