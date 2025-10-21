package utils.handle;

import com.google.protobuf.Message;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

/**
 * 抽象注册处理器
 * 处理服务器注册请求的通用逻辑,支持扩展点
 */
public abstract class AbstractRegisterHandler<T_INSTANCE> implements Handler {
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * 获取服务器单例实例
	 */
	protected abstract T_INSTANCE getServerInstance();

	/**
	 * 添加服务器客户端到管理器
	 */
	protected abstract void addServerClient(ServerType serverType, Sender client);

	/**
	 * 获取当前服务器的信息用于响应
	 */
	protected abstract ModelProto.ServerInfo getCurrentServerInfo();

	/**
	 * 注册前处理（可选扩展点）
	 */
	protected void beforeRegistration(Sender sender, ModelProto.ServerInfo serverInfo, ServerType serverType) {
	}

	/**
	 * 注册后处理（可选扩展点）
	 */
	protected void afterRegistration(Sender sender, ModelProto.ServerInfo serverInfo, ServerType serverType) {
		// 默认空实现
	}

	/**
	 * 发送注册响应（可重写以支持不同的发送方式）
	 */
	protected void sendRegistrationResponse(Sender sender, int clientId, long mapId, int sequence) {
		ModelProto.AckRegister.Builder response = ModelProto.AckRegister.newBuilder();
		response.setServerInfo(getCurrentServerInfo());
		sender.sendMessage(clientId, CMsg.ACK_REGISTER, mapId, response.build(), sequence);
		logger.debug("已发送注册响应, clientId: {}", clientId);
	}

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, long mapId, int sequence) {
		try {
			ModelProto.ReqRegister request = (ModelProto.ReqRegister) msg;
			ModelProto.ServerInfo serverInfo = request.getServerInfo();
			ServerType serverType = ServerType.get(serverInfo.getServerType());

			if (serverType == null) {
				logger.error("未知的服务器类型: {}", serverInfo.getServerType());
				return true;
			}

			logger.info("处理注册请求, serverType: {}, serverId: {}, address: {}",
					serverType, serverInfo.getServerId(),
					serverInfo.getIpConfig().toStringUtf8());

			// 注册前处理
			beforeRegistration(sender, serverInfo, serverType);

			// 处理服务注册
			processRegistration(sender, serverInfo, serverType);

			// 发送注册响应
			sendRegistrationResponse(sender, clientId, mapId, sequence);

			// 注册后处理
			afterRegistration(sender, serverInfo, serverType);

			logger.info("服务器注册成功, serverType: {}, serverId: {}",
					serverType, serverInfo.getServerId());

		} catch (Exception e) {
			logger.error("处理注册请求失败", e);
		}
		return true;
	}

	/**
	 * 处理服务器注册
	 */
	private void processRegistration(Sender sender, ModelProto.ServerInfo serverInfo, ServerType serverType) {
		addServerClient(serverType, sender);
		logger.debug("已添加服务器客户端, serverType: {}, serverId: {}", serverType, serverInfo.getServerId());
	}
}