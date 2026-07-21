package lobby.connect;

import java.util.HashMap;
import java.util.Map;

import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectProcessor {
	private static final Logger logger = LoggerFactory.getLogger(ConnectProcessor.class);

	public static final Transfer TRANSFER = (connect, message) -> false;
	public static final Parser PARSER = HandleTypeRegister::parseMessage;
	private static final Map<Integer, Handler> handlers = new HashMap<>();
	public static final Handlers HANDLERS = handlers::get;

	public static void init() {
		try {
			HandleTypeRegister.initFactory(ConnectProcessor.class, handlers);
			logger.debug("ConnectProcessor 初始化完成, 处理器数量: {}", handlers.size());
		} catch (Exception e) {
			logger.error("ConnectProcessor 初始化失败", e);
			throw new RuntimeException("ConnectProcessor初始化失败", e);
		}
	}
}
