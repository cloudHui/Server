package game.client;

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
 * 游戏客户端协议处理器
 * 负责处理网关转发消息的解析和路由
 */
public class ClientProto {
	private static final Logger logger = LoggerFactory.getLogger(ClientProto.class);

	/**
	 * 消息转发接口 - 游戏服务器不直接转发消息，返回false
	 */
	public static final Transfer TRANSFER = (client, message) -> false;

	private static final Map<Integer, Handler> HANDLER_MAP = new HashMap<>();
	public static final Handlers HANDLERS = HANDLER_MAP::get;
	public static final Parser PARSER = HandleTypeRegister::parseMessage;

	/**
	 * 初始化协议处理器
	 */
	public static void init() {
		try {
			// 绑定专用服务器消息处理
			HandleTypeRegister.initFactory(ClientProto.class, HANDLER_MAP);
			// 绑定通用服务器消息处理
			HandleTypeRegister.initFactory(HANDLER_MAP);
			logger.info("游戏客户端协议处理器初始化完成，注册处理器数量: {}", HANDLER_MAP.size());
		} catch (Exception e) {
			logger.error("游戏客户端协议处理器初始化失败", e);
			throw new RuntimeException("ClientProto初始化失败", e);
		}
	}
}