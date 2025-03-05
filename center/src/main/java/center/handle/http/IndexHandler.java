package center.handle.http;

import http.Linker;
import http.handler.Handler;
import msg.http.req.GetGateInfoRequest;
import msg.http.res.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.utils.JsonUtils;

/**
 * 首页
 */
public class IndexHandler implements Handler<GetGateInfoRequest> {

	private static final Logger LOGGER = LoggerFactory.getLogger(IndexHandler.class);
	private static final IndexHandler instance = new IndexHandler();

	private IndexHandler() {
	}

	public static IndexHandler getInstance() {
		return instance;
	}

	public String path() {
		return "index";
	}

	public GetGateInfoRequest parser(String msg) {
		return JsonUtils.readValue(msg, GetGateInfoRequest.class);
	}

	public boolean handler(Linker linker, String path, String function, GetGateInfoRequest req) {
		if (req == null) {
			return false;
		}
		long start = System.currentTimeMillis();
		Response ack = new Response();
			ack.setRet(1);
			//ack.setMsg(JsonUtils.writeValue(serverClient.getServerInfo()));
		linker.sendMessage(ack);
		start = System.currentTimeMillis() - start;
		LOGGER.info("[req:{} cost:{}ms]", req.toString(), start);
		return true;
	}
}
