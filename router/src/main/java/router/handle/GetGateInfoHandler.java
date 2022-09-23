package router.handle;

import java.util.List;

import http.Linker;
import http.handler.Handler;
import msg.http.req.GetGateInfoRequest;
import msg.http.res.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import router.Router;
import utils.utils.JsonUtils;

/**
 * 处理查询 gate 信息
 */
public class GetGateInfoHandler implements Handler<GetGateInfoRequest> {

	private static final Logger LOGGER = LoggerFactory.getLogger(GetGateInfoHandler.class);

	private static GetGateInfoHandler instance = new GetGateInfoHandler();

	private GetGateInfoHandler() {

	}

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
			String unicode = req.getUniqCode();
			Response<List<String>> ack = new Response<>();
			if (unicode != null) {
				ack.setData(Router.getInstance().getIpPortList());
			} else {
				ack.setData(Router.getInstance().getInnerIpPortList());
			}
			linker.sendMessage(ack);
			start = System.currentTimeMillis() - start;
			LOGGER.info("[req:{} start:{}ms]", req.toString(), start);
		});
		return true;
	}
}
