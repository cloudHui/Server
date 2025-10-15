package room.connect;

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
 * 连接处理器
 * 负责处理房间服务器到其他服务器的连接消息
 */
public class ConnectProcessor {
	private static final Logger logger = LoggerFactory.getLogger(ConnectProcessor.class);

	/**
	 * 消息转发接口 - 房间服务器到其他服务器的连接不支持转发
	 */
	public static final Transfer TRANSFER = (connect, message) -> {
		return false;
	};

	public static final Parser PARSER = HandleTypeRegister::parseMessage;
	private static final Map<Integer, Handler> handlers = new HashMap<>();
	public static final Handlers HANDLERS = handlers::get;

	/**
	 * 初始化连接处理器
	 */
	public static void init() {
		try {
			HandleTypeRegister.initFactory(ConnectProcessor.class, handlers);
			logger.info("连接处理器初始化完成，注册处理器数量: {}", handlers.size());
		} catch (Exception e) {
			logger.error("连接处理器初始化失败", e);
			throw new RuntimeException("ConnectProcessor初始化失败", e);
		}
	}
}