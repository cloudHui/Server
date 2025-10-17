package utils.handle;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.connect.handle.ConnectHandler;
import net.message.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.manager.ConnectHandle;
import utils.manager.HandleManager;

import java.util.List;
import java.util.function.Function;

/**
 * 抽象服务信息响应处理器
 * 处理中心服务器返回的服务信息响应，负责连接到需要的其他服务
 *
 * @param <T_SERVER> 服务器实例类型
 * @param <T_ROBOT> 定时器任务参数类型
 */
@ProcessClass(ModelProto.AckServerInfo.class)
public abstract class AbstractAckServerInfoHandle<T_SERVER, T_ROBOT> implements ConnectHandle {
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// 重试配置
	protected static final long RETRY_DELAY = 5000;
	protected static final long RETRY_INTERVAL = 1000;
	protected static final int RETRY_COUNT = 1;

	/**
	 * 获取服务器实例
	 */
	protected abstract T_SERVER getServerInstance();

	/**
	 * 获取当前服务器的类型
	 */
	protected abstract ServerType getCurrentServerType();

	/**
	 * 获取重试时请求的服务器类型列表
	 */
	protected abstract List<ServerType> getRetryServerTypes();

	/**
	 * 处理服务器信息连接
	 */
	protected abstract void processServerConnection(ModelProto.AckServerInfo response);

	/**
	 * 执行任务（用于异步执行）
	 */
	protected abstract void execute(Runnable task);

	/**
	 * 注册定时器
	 */
	protected abstract void registerTimer(long delay, long interval, int count, Function<T_ROBOT, Boolean> runner, T_ROBOT param);

	/**
	 * 获取连接处理器
	 */
	protected abstract Parser getConnectProcessor();

	@Override
	public void handle(Message message, ConnectHandler handler, int sequence, int transId) {
		try {
			if (message instanceof ModelProto.AckServerInfo) {
				ModelProto.AckServerInfo response = (ModelProto.AckServerInfo) message;

				if (response.getServersCount() > 0) {
					processServerInfo(response);
				} else {
					scheduleRetry(handler);
				}
			}
		} catch (Exception e) {
			logger.error("处理服务信息响应失败", e);
		}
	}

	/**
	 * 处理服务器信息
	 */
	private void processServerInfo(ModelProto.AckServerInfo response) {
		logger.info("处理请求需要连接的服务器信息返回, response:{}", response.toString());
		processServerConnection(response);
	}

	/**
	 * 调度重试机制
	 */
	private void scheduleRetry(ConnectHandler serverClient) {
		logger.warn("未找到可用服务器，将在 {}ms 后重试", RETRY_DELAY);

		registerTimer(RETRY_DELAY, RETRY_INTERVAL, RETRY_COUNT, robot -> {
			ModelProto.ReqServerInfo.Builder requestBuilder = ModelProto.ReqServerInfo.newBuilder();
			for (ServerType serverType : getRetryServerTypes()) {
				requestBuilder.addServerType(serverType.getServerType());
			}
			ModelProto.ReqServerInfo request = requestBuilder.build();

			HandleManager.sendMsg(CMsg.REQ_SERVER, request, serverClient, getConnectProcessor());
			return true;
		}, getRobotParameter());
	}

	/**
	 * 获取定时器任务参数
	 */
	protected abstract T_ROBOT getRobotParameter();
}