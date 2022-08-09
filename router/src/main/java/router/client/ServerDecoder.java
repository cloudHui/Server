package router.client;

import java.util.HashMap;
import java.util.Map;

import http.HttpDecoder;
import http.handler.Handler;
import router.handle.GetGateInfoHandler;
import router.handle.RegisterGateInfoHandler;

public class ServerDecoder extends HttpDecoder {
	private static final Map<String, Handler> handlers = new HashMap();

	public ServerDecoder() {
	}

	public Handler getHandler(String path) {
		return handlers.get(path);
	}

	private static void register(Handler handler) {
		handlers.put(handler.path(), handler);
	}

	static {
		register(GetGateInfoHandler.getInstance());
		register(RegisterGateInfoHandler.getInstance());
	}
}
