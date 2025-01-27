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

		TCPConnect serverClient = getTransServerClient(msgId, gateClient);
		if (serverClient != null) {
			serverClient.sendMessage(tcpMessage, 3).whenComplete((message, throwable) -> {
				if (null != throwable) {
					LOGGER.error("[ERROR! failed send transferMessage message to {} {}]", serverClient.getConnectServer(), throwable.getMessage());
				} else {
					try {
						//Todo 解决消息转发超时处理和正确转发处理
					} catch (Exception exception) {
						exception.printStackTrace();
					}
				}
			});
			return true;
		}
		LOGGER.error("[error msg transferMessage to server msgId:{}]", msgId);
		return false;
	}


	/**
	 * 获取转发服务的链接
	 */
	private static TCPConnect getTransServerClient(int msgId, GateTcpClient gateClient) {
		ServerType serverType = MessageId.getServerTypeByMessageId(msgId);
		if (serverType == null) {
			LOGGER.error("[getTransServerClient error no serverType msgId:{}]", msgId);
			return null;
		}
		int clientId = 0;
		TCPConnect serverClient;
		ServerManager server = Gate.getInstance().getServerManager();
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
			serverClient = server.getServerClient(serverType, clientId);
		} else {
			serverClient = server.getServerClient(serverType);
			switch (serverType) {
				case Game:
					gateClient.setGameId(serverClient.getConnectServer().getServerId());
					break;
				case Hall:
					gateClient.setHallId(serverClient.getConnectServer().getServerId());
					break;
				case Room:
					gateClient.setRoomId(serverClient.getConnectServer().getServerId());
					break;
			}
		}
		return serverClient;
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