package router.handle;

import java.util.List;

import http.Linker;
import http.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import router.Router;
import router.msg.req.RegisterGateInfoRequest;
import router.msg.res.Response;
import utils.utils.JsonUtils;

/**
 * 注册 gate 信息
 */
public class RegisterGateInfoHandler implements Handler<RegisterGateInfoRequest> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RegisterGateInfoHandler.class);

	private static RegisterGateInfoHandler instance = new RegisterGateInfoHandler();

	private RegisterGateInfoHandler() {

	}

	public static RegisterGateInfoHandler getInstance() {
		return instance;
	}


	public String path() {
		return "calRate";
	}

	public RegisterGateInfoRequest parser(String msg) {
		return JsonUtils.readValue(msg, RegisterGateInfoRequest.class);
	}

	public boolean handler(Linker linker, String path, String function, RegisterGateInfoRequest req) {
		if (req == null) {
			return false;
		}
		Router.getInstance().execute(() -> {
			long start = System.currentTimeMillis();
			List<String> ipPort = req.getIpPort();
			List<String> innerIpPort = req.getInnerIpPort();
			Response ack = new Response();
			if (ipPort != null && innerIpPort != null && !ipPort.isEmpty() && !innerIpPort.isEmpty()
					&& ipPort.size() == innerIpPort.size()) {
				Router.getInstance().getIpPortList().addAll(ipPort);
				Router.getInstance().getIpPortList().addAll(innerIpPort);
			} else {
				ack.setMsg("[RegisterGateInfoRequest error msg null msg: " + req.toString() + " ]");
			}
			linker.sendMessage(ack);
			start = System.currentTimeMillis() - start;
			LOGGER.info("[req:{} start:{}ms]", req.toString(), start);
		});
		return true;
	}
}
