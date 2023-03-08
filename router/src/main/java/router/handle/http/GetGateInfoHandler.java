package router.handle.http;

import http.Linker;
import http.handler.Handler;
import msg.ServerType;
import msg.http.req.GetGateInfoRequest;
import msg.http.res.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import router.Router;
import router.client.RouterClient;
import router.manager.ServerManager;
import utils.utils.JsonUtils;

/**
 * 处理查询 gate 信息
 */
public class GetGateInfoHandler implements Handler<GetGateInfoRequest> {

	private GetGateInfoHandler() {
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(GetGateInfoHandler.class);

	private static GetGateInfoHandler instance = new GetGateInfoHandler();

	public static GetGateInfoHandler getInstance() {
		return instance;
	}

	public String path() {
		return "calRate";
	}

	public GetGateInfoRequest parser(String msg) {
		return JsonUtils.readValue(msg, GetGateInfoRequest.class);
	}

	public boolean handler(Linker linker, String path, String function, GetGateInfoRequest req) {
		if (req == null) {
			return false;
		}
		Router.getInstance().execute(() -> {
			long start = System.currentTimeMillis();
			Response ack = new Response();
			ServerManager instance = ServerManager.getInstance();
			RouterClient serverClient = instance.getServerClient(ServerType.Gate);
			if (serverClient != null) {
				ack.setRet(1);
				ack.setMsg(serverClient.getIpConfig());
			}
			linker.sendMessage(ack);
			start = System.currentTimeMillis() - start;
			LOGGER.info("[req:{} cost:{}ms]", req.toString(), start);
		});
		return true;
	}
}
