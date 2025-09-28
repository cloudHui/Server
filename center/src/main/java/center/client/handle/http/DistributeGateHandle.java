package center.client.handle.http;

import center.Center;
import center.client.CenterClient;
import center.client.handle.NotClientLinkHandle;
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

	public String path() {
		return "divide";
	}

	public String parser(String msg) {
		return msg;
	}

	public boolean handler(Linker linker, String req) {
		Response ack = new Response();
		String gate = NotClientLinkHandle.getLinkGate(linker.remoteIp());

		int result = 1;
		if (gate == null) {
			CenterClient serverClient = (CenterClient) Center.getInstance().getServerManager().getServerClient(ServerType.Gate);
			if (serverClient != null) {
				gate = serverClient.getServerInfo().getIpConfig().toStringUtf8();
			} else {
				result = 0;
			}
		}
		ack.setRet(result);
		ack.setMsg(gate);
		linker.sendMessage(ack);
		LOGGER.info("{} remote {}", path(), linker.remoteIp());
		return false;
	}
}
