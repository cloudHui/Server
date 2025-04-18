package gate.client;

import gate.Gate;
import msg.MessageId;
import msg.ServerType;
import net.connect.handle.ConnectHandler;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.ServerManager;

public class ClientProto {
	private final static Logger LOGGER = LoggerFactory.getLogger(ClientProto.class);

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (gateClient, tcpMessage) -> transferMessage((GateTcpClient) gateClient, tcpMessage);


	/**
	 * 消息转发到服务器
	 */
	private static boolean transferMessage(GateTcpClient gateClient, TCPMessage tcpMessage) {
		int msgId = tcpMessage.getMessageId();

		tcpMessage.setMapId(gateClient.getId());
		tcpMessage.setClientId(gateClient.getId());

		ConnectHandler connect = getTransServerClient(msgId, gateClient);
		if (connect != null) {
			connect.sendMessage(tcpMessage, 3).whenComplete((message, throwable) ->
					LOGGER.info("[send transferMessage message to {} {} success:{}]",
					connect.getConnectServer(), msgId, throwable == null ? true : throwable.getMessage()));
			return true;
		}
		LOGGER.error("[error msg transferMessage to server msgId:{}]", msgId);
		return false;
	}


	/**
	 * 获取转发服务的链接
	 */
	private static ConnectHandler getTransServerClient(int msgId, GateTcpClient gateClient) {
		ServerType serverType = MessageId.getServerTypeByMessageId(msgId);
		if (serverType == null) {
			LOGGER.error("[getTransServerClient error no serverType msgId:{}]", msgId);
			return null;
		}
		int clientId = 0;
		ConnectHandler server;
		ServerManager manager = Gate.getInstance().getServerManager();
		switch (serverType) {
			case Game:
				clientId = gateClient.getGameId();
				break;
			case Hall:
				clientId = gateClient.getHallId();
				break;
			case Room:
				clientId = gateClient.getRoomId();
				break;
		}
		if (clientId != 0) {
			server = manager.getServerClient(serverType, clientId);
		} else {
			server = manager.getServerClient(serverType);
			switch (serverType) {
				case Game:
					gateClient.setGameId(server.getConnectServer().getServerId());
					break;
				case Hall:
					gateClient.setHallId(server.getConnectServer().getServerId());
					break;
				case Room:
					gateClient.setRoomId(server.getConnectServer().getServerId());
					break;
			}
		}
		return server;
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
		ConnectHandler serverClient = serverManager.getServerClient(ServerType.Game, gameId);
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