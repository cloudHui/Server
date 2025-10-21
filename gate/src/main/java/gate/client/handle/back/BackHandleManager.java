package gate.client.handle.back;

import java.util.HashMap;
import java.util.Map;

import gate.client.GateTcpClient;
import msg.registor.HandleTypeRegister;
import net.message.TCPMessage;

/**
 * @author admin
 * @className BackHandleManager
 * @description
 * @createDate 2025/10/21 15:35
 */
public class BackHandleManager {

	private static final Map<Integer, BackHandle> BACK_HANDLE_MAP = new HashMap<>();

	public static void init() {
		HandleTypeRegister.initFactory(BackHandleManager.class, BACK_HANDLE_MAP);
	}

	/**
	 * 统一处理客户端异步消息返回数据处理
	 *
	 * @param response 异步消息
	 * @param client   链接
	 */
	public static void handle(TCPMessage response, GateTcpClient client) {
		BackHandle backHandle = BACK_HANDLE_MAP.get(response.getMessageId());

		if (backHandle == null) {
			return;
		}
		backHandle.handle(response, client);
	}
}
