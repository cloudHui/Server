package utils.handle;

import java.util.List;

import com.google.protobuf.Message;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.connect.handle.ConnectHandler;
import net.message.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;
import utils.manager.ConnectHandle;
import utils.manager.HandleManager;

/**
 * 通用的服务器信息响应处理基类
 */
public abstract class AbstractAckServerInfoHandle implements ConnectHandle {
	protected static final Logger logger = LoggerFactory.getLogger(AbstractAckServerInfoHandle.class);

	// 重试配置
	protected static final long RETRY_DELAY = 5000;
	protected static final long RETRY_INTERVAL = 1000;
	protected static final int RETRY_COUNT = 1;

	@Override
	public void handle(Message message, ConnectHandler handler, int sequence, int transId) {
		try {
			if (message instanceof ServerProto.AckServerInfo) {
				ServerProto.AckServerInfo response = (ServerProto.AckServerInfo) message;

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
	 * 处理服务器信息 - 由子类实现
	 */
	protected abstract void processServerInfo(ServerProto.AckServerInfo response);

	/**
	 * 调度重试机制 - 由子类实现
	 */
	protected abstract void scheduleRetry(ConnectHandler serverClient);

	/**
	 * 通用的重试请求构建方法
	 */
	protected void sendRetryRequest(ConnectHandler serverClient, List<ServerType> serverTypes,
									Parser processor) {
		ServerProto.ReqServerInfo.Builder builder = ServerProto.ReqServerInfo.newBuilder();
		for (ServerType serverType : serverTypes) {
			builder.addServerType(serverType.getServerType());
		}

		HandleManager.sendMsg(CMsg.REQ_SERVER, builder.build(), serverClient, processor);
	}
}