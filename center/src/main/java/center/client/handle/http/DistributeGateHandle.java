package center.client.handle.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import center.Center;
import center.client.CenterClient;
import center.client.handle.NotClientLinkHandle;
import http.Linker;
import http.handler.Handler;
import msg.http.res.Response;
import msg.registor.enums.ServerType;

/**
 * HTTP接口：为客户端分配网关服务器
 * 实现负载均衡和会话保持
 */
public class DistributeGateHandle implements Handler<String> {
	private static final Logger logger = LoggerFactory.getLogger(DistributeGateHandle.class);

	public String path() {
		return "divide";
	}

	public String parser(String message) {
		return message;
	}

	public boolean handler(Linker linker, String request) {
		try {
			String clientIp = linker.remoteIp();
			logger.info("处理网关分配请求, clientIp: {}", clientIp);

			Response response = allocateGateway(clientIp);
			linker.sendMessage(response);

			logger.info("网关分配完成, clientIp: {}, result: {}, gate: {}", clientIp, response.getRet(), response.getMsg());
			return false;
		} catch (Exception e) {
			logger.error("处理网关分配请求失败, clientIp: {}", linker.remoteIp(), e);
			return false;
		}
	}

	/**
	 * 为客户端分配网关服务器
	 */
	private Response allocateGateway(String clientIp) {
		Response response = new Response();

		// 首先检查客户端是否已有连接的网关（会话保持）
		String existingGate = NotClientLinkHandle.getClientGate(clientIp);
		if (existingGate != null) {
			logger.debug("客户端使用现有网关连接, clientIp: {}, gate: {}", clientIp, existingGate);
			response.setRet(1);
			response.setMsg(existingGate);
			return response;
		}

		// 选择可用的网关服务器（简单的负载均衡）
		CenterClient gateway = selectAvailableGateway();
		if (gateway != null) {
			String gateAddress = gateway.getServerInfo().getIpConfig().toStringUtf8();
			response.setRet(1);
			response.setMsg(gateAddress);
			logger.debug("为客户端分配新网关, clientIp: {}, gate: {}", clientIp, gateAddress);
		} else {
			response.setRet(0);
			response.setMsg("No available gateway");
			logger.warn("没有可用的网关服务器, clientIp: {}", clientIp);
		}

		return response;
	}

	/**
	 * 选择可用的网关服务器
	 * 这里使用简单的随机选择,可以扩展为更复杂的负载均衡算法
	 */
	private CenterClient selectAvailableGateway() {
		return (CenterClient) Center.getInstance().getServerManager().getServerClient(ServerType.Gate);
	}
}