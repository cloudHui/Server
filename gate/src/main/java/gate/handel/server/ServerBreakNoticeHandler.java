package gate.handel.server;

import java.util.List;

import gate.Gate;
import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.ServerManager;

/**
 * 服务掉线通知
 */
public class ServerBreakNoticeHandler implements Handler<ModelProto.NotServerBreak> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerBreakNoticeHandler.class);

	private static ServerBreakNoticeHandler instance = new ServerBreakNoticeHandler();

	public static ServerBreakNoticeHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, ModelProto.NotServerBreak req, int mapId) {
		return connectToSever(req.getServersList());
	}

	static boolean connectToSever(List<ModelProto.ServerInfo> serverInfos) {
		if (serverInfos == null || serverInfos.isEmpty()) {
			return true;
		}
		Gate instance = Gate.getInstance();
		ServerManager serverManager = instance.getServerManager();
		ServerType serverType;
		for (ModelProto.ServerInfo serverInfo : serverInfos) {
			serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				serverManager.removeServerClient(serverType, serverInfo.getServerId());
				LOGGER.error("[gate receive server:{} info:{} break]", serverType, serverInfo.toString());
			}
		}
		return true;
	}
}
