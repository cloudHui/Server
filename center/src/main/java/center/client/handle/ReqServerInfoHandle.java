package center.client.handle;

import java.util.List;

import center.Center;
import center.client.CenterClient;
import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.client.handler.ClientHandler;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.ServerClientManager;

/**
 * 处理服务信息查询请求
 * 返回指定类型服务器的信息列表
 */
@ProcessType(CMsg.REQ_SERVER)
public class ReqServerInfoHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqServerInfoHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, int mapId, long sequence) {
		try {
			ModelProto.ReqServerInfo request = (ModelProto.ReqServerInfo) message;
			ServerClientManager manager = Center.getInstance().getServerManager();

			logger.debug("处理服务信息查询请求, 查询类型数量: {}", request.getServerTypeCount());

			ModelProto.AckServerInfo response = buildServerInfoResponse(request, manager);
			sender.sendMessage(clientId, CMsg.ACK_SERVER, mapId, response, sequence);

			logger.info("返回服务信息, 服务器数量: {}", response.getServersCount());
			return true;
		} catch (Exception e) {
			logger.error("处理服务信息查询请求失败", e);
			return false;
		}
	}

	/**
	 * 构建服务器信息响应
	 */
	private ModelProto.AckServerInfo buildServerInfoResponse(ModelProto.ReqServerInfo request, ServerClientManager manager) {
		ModelProto.AckServerInfo.Builder response = ModelProto.AckServerInfo.newBuilder();
		List<Integer> serverTypes = request.getServerTypeList();

		for (int serverTypeValue : serverTypes) {
			ServerType serverType = ServerType.get(serverTypeValue);
			if (serverType == null) {
				logger.warn("未知的服务器类型值: {}", serverTypeValue);
				continue;
			}

			addServerInfoToResponse(manager, response, serverType);
		}

		return response.build();
	}

	/**
	 * 添加指定类型的服务器信息到响应
	 */
	private void addServerInfoToResponse(ServerClientManager manager,
										 ModelProto.AckServerInfo.Builder response,
										 ServerType serverType) {
		List<ClientHandler> servers = manager.getAllTypeServer(serverType);
		if (servers == null || servers.isEmpty()) {
			logger.debug("该类型服务器暂无在线实例: {}", serverType);
			return;
		}

		int addedCount = 0;
		for (ClientHandler client : servers) {
			CenterClient centerClient = (CenterClient) client;
			if (centerClient.getServerInfo() != null) {
				response.addServers(centerClient.getServerInfo());
				addedCount++;
			}
		}

		logger.debug("添加服务器信息到响应, serverType: {}, 数量: {}", serverType, addedCount);
	}
}