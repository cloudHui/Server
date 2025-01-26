package gate.client;

import gate.Gate;
import msg.MessageId;
import msg.ServerType;
import net.connect.TCPConnect;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.ServerManager;

public class ClientProto {
	private final static Logger logger = LoggerFactory.getLogger(ClientProto.class);

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (gateClient, tcpMessage) -> transferMessage((GateTcpClient) gateClient, tcpMessage);


	/**
	 * 消息转发到服务器
	 */
	private static boolean transferMessage(GateTcpClient gateClient, TCPMessage tcpMessage) {
		int msgId = tcpMessage.getMessageId();
		//奇数消息是发给服务的
		ServerManager server = Gate.getInstance().getServerManager();
		tcpMessage.setMapId(gateClient.getId());
		tcpMessage.setClientId(gateClient.getId());
		int clientId;
		TCPConnect serverClient;
		if ((msgId & MessageId.GAME_TYPE) != 0) {
			clientId = gateClient.getGameId();
			if (clientId != 0) {
				serverClient = server.getServerClient(ServerType.Game, clientId);
			} else {
				serverClient = server.getServerClient(ServerType.Game);
				gateClient.setGameId(serverClient.getConnectServer().getServerId());
			}
			if (serverClient != null) {
				serverClient.sendMessage(tcpMessage);
				return true;
			} else {
				logger.error("[transform game msg error clientId :{} can‘t find game server]", clientId);
			}
		} else if ((msgId & MessageId.HALL_TYPE) != 0) {
			clientId = gateClient.getHallId();
			if (clientId != 0) {
				serverClient = server.getServerClient(ServerType.Hall, clientId);
			} else {
				serverClient = server.getServerClient(ServerType.Hall);
				gateClient.setHallId(serverClient.getConnectServer().getServerId());
			}
			if (serverClient != null) {
				serverClient.sendMessage(tcpMessage);
				return true;
			} else {
				logger.error("[transform hall msg error clientId :{} can‘t find hall server]", clientId);
			}
		} else if ((msgId & MessageId.ROOM_TYPE) != 0) {
			clientId = gateClient.getRoomId();
			if (clientId != 0) {
				serverClient = server.getServerClient(ServerType.Room, clientId);
			} else {
				serverClient = server.getServerClient(ServerType.Room);
				gateClient.setRoomId(serverClient.getConnectServer().getServerId());
			}
			if (serverClient != null) {
				serverClient.sendMessage(tcpMessage);
				return true;
			} else {
				logger.error("[transform room msg error clientId :{} can‘t find room server]", clientId);
			}
		}
		logger.error("[error msg transferMessage to server msgId:{}]", msgId);
		return false;
	}


	/**
	 * 通知服务器玩家离线
	 */
	protected static void notServerBreak(int userId, int gameId, int hallId, int roomId) {
		ModelProto.NotBreak.Builder not = ModelProto.NotBreak.newBuilder();
		not.setUserId(userId);
		ServerManager serverManager = Gate.getInstance().getServerManager();
		if (serverManager == null) {
			return;
		}
		TCPConnect serverClient = serverManager.getServerClient(ServerType.Game, gameId);
		if (serverClient != null) {
			serverClient.sendMessage(MessageId.NOT_BREAK, not.build());
		}
		serverClient = serverManager.getServerClient(ServerType.Hall, hallId);
		if (serverClient != null) {
			serverClient.sendMessage(MessageId.NOT_BREAK, not.build());
		}
		serverClient = serverManager.getServerClient(ServerType.Room, roomId);
		if (serverClient != null) {
			serverClient.sendMessage(MessageId.NOT_BREAK, not.build());
		}
	}
}