package gate.handle;

import java.util.List;

import com.google.protobuf.Message;
import gate.Gate;
import msg.MessageId;
import msg.ServerType;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.ServerManager;

/**
 * 服务掉线通知
 */
@ProcessType(MessageId.BREAK_NOTICE)
public class ServerBreakNoticeHandler implements Handler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerBreakNoticeHandler.class);

	@Override
	public boolean handler(Sender sender, int aLong, Message notServerBreak, int mapId, long sequence) {
		ModelProto.NotServerBreak req = (ModelProto.NotServerBreak) notServerBreak;
		List<ModelProto.ServerInfo> serverInfos = req.getServersList();
		if (serverInfos.isEmpty()) {
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
