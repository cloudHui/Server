package lobby.connect.center.notice;

import java.util.List;

import com.google.protobuf.Message;
import lobby.Lobby;
import lobby.manager.table.TableManager;
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

@ProcessType(CMsg.BREAK_NOTICE)
public class ServerBreakNoticeHandle implements Handler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServerBreakNoticeHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message notServerBreak, long mapId, int sequence) {
		ServerProto.NotServerBreak req = (ServerProto.NotServerBreak) notServerBreak;
		List<ModelProto.ServerInfo> serverInfos = req.getServersList();
		if (serverInfos.isEmpty()) {
			return true;
		}
		ServerManager serverManager = Lobby.getInstance().getServerManager();
		for (ModelProto.ServerInfo serverInfo : serverInfos) {
			ServerType serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				serverManager.removeServerClient(serverType, serverInfo.getServerId());
				LOGGER.error("[lobby receive server:{} info:{} break]", serverType, serverInfo.toString());
				if (serverType == ServerType.Game) {
					TableManager.getInstance().clearAllTables();
					LOGGER.warn("Game断线,已清理所有桌子");
				}
			}
		}
		return true;
	}
}
