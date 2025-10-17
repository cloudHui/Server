package center.client.handle;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 处理网关服务器通知的客户端连接事件
 * 跟踪客户端到网关的映射关系，用于负载均衡
 */
@ProcessType(CMsg.NOT_LINK)
public class NotClientLinkHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(NotClientLinkHandle.class);

	// 客户端IP到网关地址的映射
	private static final ConcurrentHashMap<String, String> clientToGateMap = new ConcurrentHashMap<>();

	/**
	 * 客户端断开连接
	 */
	public static void clientDisconnect(String clientIp) {
		String removedGate = clientToGateMap.remove(clientIp);
		if (removedGate != null) {
			logger.info("客户端断开连接, clientIp: {}, gate: {}", clientIp, removedGate);
		} else {
			logger.error("客户端断开连接但未找到映射, clientIp: {}", clientIp);
		}
	}

	/**
	 * 客户端建立连接
	 */
	public static void addClientConnection(String clientIp, String gateAddress) {
		if (clientIp == null || gateAddress == null) {
			logger.error("无效的客户端连接参数, clientIp: {}, gate: {}", clientIp, gateAddress);
			return;
		}

		String previousGate = clientToGateMap.put(clientIp, gateAddress);
		if (previousGate != null) {
			logger.info("客户端切换网关, clientIp: {}, 旧网关: {}, 新网关: {}",
					clientIp, previousGate, gateAddress);
		} else {
			logger.info("客户端连接新网关, clientIp: {}, gate: {}", clientIp, gateAddress);
		}
	}

	/**
	 * 获取客户端连接的网关地址
	 */
	public static String getClientGate(String clientIp) {
		return clientToGateMap.get(clientIp);
	}

	/**
	 * 获取当前连接的客户端数量
	 */
	public static int getClientCount() {
		return clientToGateMap.size();
	}

	@Override
	public boolean handler(Sender sender, int clientId, Message message, int mapId, int sequence) {
		try {
			ModelProto.NotRegisterClient notification = (ModelProto.NotRegisterClient) message;
			String clientIp = notification.getCert().toStringUtf8();
			String gateAddress = notification.getGate().toStringUtf8();

			addClientConnection(clientIp, gateAddress);
			return true;
		} catch (Exception e) {
			logger.error("处理客户端连接通知失败, clientId: {}", clientId, e);
			return false;
		}
	}
}