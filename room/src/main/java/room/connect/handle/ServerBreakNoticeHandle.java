package room.connect.handle;

import java.util.List;

import com.google.protobuf.Message;
import msg.registor.message.CMsg;
import msg.registor.enums.ServerType;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import room.Room;
import utils.ServerManager;

/**
 * 服务掉线通知
 */
@ProcessType(CMsg.BREAK_NOTICE)
public class ServerBreakNoticeHandle implements Handler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerBreakNoticeHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message notServerBreak, int mapId, long sequence) {
		ModelProto.NotServerBreak req = (ModelProto.NotServerBreak) notServerBreak;
		List<ModelProto.ServerInfo> serverInfos = req.getServersList();
		if (serverInfos.isEmpty()) {
			return true;
		}
		Room instance = Room.getInstance();
		ServerManager serverManager = instance.getServerManager();
		ServerType serverType;
		for (ModelProto.ServerInfo serverInfo : serverInfos) {
			serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				serverManager.removeServerClient(serverType, serverInfo.getServerId());
				LOGGER.error("[room receive server:{} info:{} break]", serverType, serverInfo.toString());
			}
		}
		return true;
	}
}
