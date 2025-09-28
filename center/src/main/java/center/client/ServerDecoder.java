package center.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import http.HttpDecoder;
import http.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.other.ClazzUtil;

public class ServerDecoder extends HttpDecoder {

	private static final Logger logger = LoggerFactory.getLogger(ServerDecoder.class);

	private static final Map<String, Handler<?>> handlers = new HashMap<>();

	public ServerDecoder() {
	}

	public Handler<?> getHandler(String path) {
		return handlers.get(path);
	}

	private static void register(Handler<?> handler) {
		handlers.put(handler.path(), handler);
	}

	public static void init() {
		Class<ServerDecoder> packageClass = ServerDecoder.class;
		try {
			List<Class<?>> classes = ClazzUtil.getAllClassExceptPackageClass(packageClass, "");
			for (Class<?> aClass : classes) {
				if (Handler.class.isAssignableFrom(aClass)) {
					register((Handler<?>) aClass.getConstructor().newInstance());
				}
			}
		} catch (Exception e) {
			logger.error("HttpDecoder init error {} ", packageClass.getName(), e);
			e.printStackTrace();
		}
	}
}
