package center.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import http.HttpDecoder;
import http.handler.Handler;
import utils.other.ClazzUtil;

/**
 * HTTP请求解码器
 * 负责将HTTP请求路由到相应的处理器
 */
public class ServerDecoder extends HttpDecoder {
	private static final Logger logger = LoggerFactory.getLogger(ServerDecoder.class);

	private static final Map<String, Handler<?>> handlers = new HashMap<>();

	static {
		initializeHandlers();
	}

	public ServerDecoder() {
		logger.debug("创建HTTP请求解码器");
	}

	/**
	 * 初始化所有HTTP请求处理器
	 */
	private static void initializeHandlers() {
		try {
			Class<ServerDecoder> packageClass = ServerDecoder.class;
			List<Class<?>> classes = ClazzUtil.getAllClassExceptPackageClass(packageClass, "");

			int handlerCount = 0;
			for (Class<?> clazz : classes) {
				if (Handler.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
					registerHandler(clazz);
					handlerCount++;
				}
			}

			logger.info("HTTP请求处理器初始化完成,注册数量: {}", handlerCount);
		} catch (Exception e) {
			logger.error("HTTP请求处理器初始化失败", e);
			throw new RuntimeException("ServerDecoder初始化失败", e);
		}
	}

	/**
	 * 注册单个处理器
	 */
	private static void registerHandler(Class<?> handlerClass) {
		try {
			Handler<?> handler = (Handler<?>) handlerClass.getConstructor().newInstance();
			String path = handler.path();

			if (handlers.containsKey(path)) {
				logger.warn("HTTP处理器路径冲突: {}, 类: {}", path, handlerClass.getName());
				return;
			}

			handlers.put(path, handler);
			logger.debug("注册HTTP处理器, path: {}, class: {}", path, handlerClass.getName());
		} catch (Exception e) {
			logger.error("注册HTTP处理器失败, class: {}", handlerClass.getName(), e);
		}
	}

	public Handler<?> getHandler(String path) {
		Handler<?> handler = handlers.get(path);
		if (handler == null) {
			logger.debug("未找到对应的HTTP处理器, path: {}", path);
		}
		return handler;
	}
}