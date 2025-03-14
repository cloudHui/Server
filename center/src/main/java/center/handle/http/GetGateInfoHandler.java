package center.handle.http;

import center.Center;
import center.client.CenterClient;
import http.Linker;
import http.handler.Handler;
import msg.ServerType;
import msg.http.res.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.utils.JsonUtils;

/**
 * 处理查询 gate 信息
 */
public class GetGateInfoHandler implements Handler<String> {

	private static final Logger LOGGER = LoggerFactory.getLogger(GetGateInfoHandler.class);
	private static final GetGateInfoHandler instance = new GetGateInfoHandler();

	private GetGateInfoHandler() {
	}

	public static GetGateInfoHandler getInstance() {
		return instance;
	}

	public String path() {
		return "getGate";
	}

	public String parser(String msg) {
		return msg;
	}

	public boolean handler(Linker linker, String path, String function, String req) {
		long start = System.currentTimeMillis();
		Response ack = new Response();
		CenterClient serverClient = (CenterClient) Center.getInstance().getServerManager().getServerClient(ServerType.Gate);
		if (serverClient != null) {
			ack.setRet(1);
			ack.setMsg(JsonUtils.writeValue(serverClient.getServerInfo()));
		}
		linker.sendMessage(ack);
		start = System.currentTimeMillis() - start;
		LOGGER.info("[cost:{}ms]", start);
		return true;
	}
}
