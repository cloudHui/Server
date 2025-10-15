package center.client.handle.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import center.Center;
import center.client.CenterClient;
import http.Linker;
import http.handler.Handler;
import msg.http.res.Response;
import msg.registor.enums.ServerType;
import utils.other.JsonUtils;

/**
 * HTTP接口：查询网关服务器信息
 */
public class GetGateInfoHandle implements Handler<String> {
	private static final Logger logger = LoggerFactory.getLogger(GetGateInfoHandle.class);

	public String path() {
		return "getGate";
	}

	public String parser(String message) {
		return message;
	}

	public boolean handler(Linker linker, String request) {
		try {
			logger.info("处理网关信息查询请求, clientIp: {}", linker.remoteIp());

			Response response = getGatewayInfo();
			linker.sendMessage(response);

			logger.info("网关信息查询完成, clientIp: {}", linker.remoteIp());
			return true;
		} catch (Exception e) {
			logger.error("处理网关信息查询请求失败, clientIp: {}", linker.remoteIp(), e);
			return false;
		}
	}

	/**
	 * 获取网关服务器信息
	 */
	private Response getGatewayInfo() {
		Response response = new Response();

		CenterClient gateway = (CenterClient) Center.getInstance().getServerManager().getServerClient(ServerType.Gate);
		if (gateway != null && gateway.getServerInfo() != null) {
			response.setRet(1);
			response.setMsg(JsonUtils.writeValue(gateway.getServerInfo()));
			logger.debug("返回网关服务器信息, serverId: {}", gateway.getServerInfo().getServerId());
		} else {
			response.setRet(0);
			response.setMsg("No gateway available");
			logger.warn("没有可用的网关服务器");
		}

		return response;
	}
}