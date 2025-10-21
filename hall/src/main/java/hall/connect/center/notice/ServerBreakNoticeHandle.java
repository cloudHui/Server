package hall.connect.center.notice;

import java.util.List;

import com.google.protobuf.Message;
import hall.Hall;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import proto.ServerProto;
import utils.ServerManager;

/**
 * 服务掉线通知
 */
@ProcessType(CMsg.BREAK_NOTICE)
public class ServerBreakNoticeHandle implements Handler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerBreakNoticeHandle.class);

	@Override
	public boolean handler(Sender sender, int aLong, Message notServerBreak, long mapId, int sequence) {
		ServerProto.NotServerBreak req = (ServerProto.NotServerBreak) notServerBreak;
		List<ModelProto.ServerInfo> serverInfos = req.getServersList();
		if (serverInfos.isEmpty()) {
			return true;
		}
		Hall instance = Hall.getInstance();
		ServerManager serverManager = instance.getServerManager();
		ServerType serverType;
		for (ModelProto.ServerInfo serverInfo : serverInfos) {
			serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				serverManager.removeServerClient(serverType, serverInfo.getServerId());
				LOGGER.error("[hall receive server:{} info:{} break]", serverType, serverInfo.toString());
			}
		}
		return true;
	}
}
