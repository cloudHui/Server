package center.client;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;

/**
 * 中心服务器客户端协议处理器
 * 负责消息解析和处理器注册
 */
public class ClientProto {
	private static final Logger logger = LoggerFactory.getLogger(ClientProto.class);

	/**
	 * 消息转发接口 - 中心服务器不直接转发消息，返回false
	 */
	public static final Transfer TRANSFER = (client, message) -> false;

	private static final Map<Integer, Handler> handlers = new HashMap<>();
	public static final Handlers HANDLERS = handlers::get;
	public static final Parser PARSER = HandleTypeRegister::parseMessage;

	/**
	 * 初始化协议处理器
	 */
	public static void init() {
		try {
			// 绑定专用服务器消息处理
			HandleTypeRegister.initFactory(ClientProto.class, handlers);
			// 绑定通用服务器消息处理
			HandleTypeRegister.initFactory(handlers);
			logger.info("中心服务器协议处理器初始化完成，注册处理器数量: {}", handlers.size());
		} catch (Exception e) {
			logger.error("中心服务器协议处理器初始化失败", e);
			throw new RuntimeException("ClientProto初始化失败", e);
		}
	}
}