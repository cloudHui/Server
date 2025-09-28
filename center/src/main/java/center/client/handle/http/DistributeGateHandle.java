package center.client.handle.http;

import java.util.concurrent.ConcurrentHashMap;

import center.Center;
import center.client.CenterClient;
import http.Linker;
import http.handler.Handler;
import msg.http.res.Response;
import msg.registor.enums.ServerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分配gate
 */
public class DistributeGateHandle implements Handler<String> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DistributeGateHandle.class);

	private static final ConcurrentHashMap<String, String> clientToGate = new ConcurrentHashMap<>();

	public String path() {
		return "divide";
	}

	public String parser(String msg) {
		return msg;
	}

	public boolean handler(Linker linker, String req) {
		Response ack = new Response();
		String gate = clientToGate.get(linker.remoteIp());

		int result = 1;
		if (gate == null) {
			CenterClient serverClient = (CenterClient) Center.getInstance().getServerManager().getServerClient(ServerType.Gate);
			if (serverClient != null) {
				gate = serverClient.getServerInfo().getIpConfig().toStringUtf8();
				clientToGate.put(linker.remoteIp(), ack.getMsg());
			} else {
				result = 0;
			}
		}
		ack.setRet(result);
		ack.setMsg(gate);
		linker.sendMessage(ack);
		LOGGER.info("remote {}", linker.remoteIp());
		return false;
	}


	/**
	 * 客户端断线
	 */
	public static void clientDisconnect(String ip) {
		clientToGate.remove(ip);
	}
}
