package center.client;

import java.util.HashMap;
import java.util.Map;

import center.handel.http.GetGateInfoHandler;
import http.HttpDecoder;
import http.handler.Handler;

public class ServerDecoder extends HttpDecoder {
	private static final Map<String, Handler<?>> handlers = new HashMap<>();

	public ServerDecoder() {
	}

	public Handler<?> getHandler(String path) {
		return handlers.get(path);
	}

	private static void register(Handler<?> handler) {
		handlers.put(handler.path(), handler);
	}

	static {
		register(GetGateInfoHandler.getInstance());
	}
}
