package router.handle;

import http.Linker;
import http.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import router.msg.req.CTRGetGateInfo;
import router.msg.res.Response;

/**
 * 处理客户端查询 gate 信息
 */
public class CTRGetGateInfoHandler implements Handler<CTRGetGateInfo> {

	private static final Logger LOGGER = LoggerFactory.getLogger(CTRGetGateInfoHandler.class);

	private static CTRGetGateInfoHandler instance = new CTRGetGateInfoHandler();

	private CTRGetGateInfoHandler() {

	}

	public static CTRGetGateInfoHandler getInstance() {
		return instance;
	}


	public String path() {
		return "calRate";
	}

	public CTRGetGateInfo parser(String msg) {
		CTRGetGateInfo rate = new CTRGetGateInfo();
		return rate;
	}

	public boolean handler(Linker linker, String path, String function, CTRGetGateInfo req) {
		long start = System.currentTimeMillis();
		Response ack = new Response();

		linker.sendMessage(ack);
		start = System.currentTimeMillis() - start;
		LOGGER.info("[req:{}]", req.toString());
		return false;
	}
}
