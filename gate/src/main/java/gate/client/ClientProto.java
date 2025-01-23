package gate.client;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.MessageLite;
import gate.Gate;
import msg.MessageId;
import msg.ServerType;
import net.connect.TCPConnect;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ServerManager;

public class ClientProto {
	private final static Logger logger = LoggerFactory.getLogger(ClientProto.class);

	/**
	 * 消息转化接口
	 */
	public final static Parser PARSER = ClientProto::parserMessage;

	/**
	 * 消息转化
	 */
	private static com.google.protobuf.Message parserMessage(int id, byte[] bytes) {
		MessageId.GateMsg gateMsg = MessageId.GateMsg.get(id);
		if (gateMsg != null) {
			Class<?> className = gateMsg.getClassName();
			try {
				return (com.google.protobuf.Message) MessageId.getMessageObject((Class<MessageLite>) className, bytes);
			} catch (Exception e) {
				logger.error("parse message error messageId :{} className:{}", id, className.getSimpleName());
			}
		}

		return null;
	}

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
	}

	/**
	 * 消息处理接口
	 */
	public final static Handlers HANDLERS = handlers::get;


	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (gateClient, tcpMessage) -> {
		int msgId = tcpMessage.getMessageId();
		if (msgId > MessageId.BASE_ID_INDEX) {
			return transferMessage((GateTcpClient)gateClient, tcpMessage, msgId);
		}
		return false;
	};


	/**
	 * 消息转发到服务器
	 */
	private static boolean transferMessage(GateTcpClient gateClient, TCPMessage tcpMessage, int msgId) {
		//奇数消息是发给服务的
		ServerManager serverManager = Gate.getInstance().getServerManager();
		tcpMessage.setMapId((int) gateClient.getId());
		tcpMessage.setClientId(gateClient.getRoleId());
		int clientId;
		TCPConnect serverClient;
		if ((msgId & MessageId.GAME_TYPE) != 0) {
			clientId = gateClient.getGameId();
			if (clientId != 0) {
				serverClient = serverManager.getServerClient(ServerType.Game, clientId);
			} else {
				serverClient = serverManager.getServerClient(ServerType.Game);
				gateClient.setGameId((int) serverClient.getServerId());
			}
			if (serverClient != null) {
				serverClient.sendMessage(tcpMessage);
				return true;
			} else {
				logger.error("transform game msg error clientId :{} can‘t find game server", clientId);
			}
		} else if ((msgId & MessageId.HALL_TYPE) != 0) {
			clientId = gateClient.getHallId();
			if (clientId != 0) {
				serverClient = serverManager.getServerClient(ServerType.Hall, clientId);
			} else {
				serverClient = serverManager.getServerClient(ServerType.Hall);
				gateClient.setHallId((int) serverClient.getServerId());
			}
			if (serverClient != null) {
				serverClient.sendMessage(tcpMessage);
				return true;
			} else {
				logger.error("transform hall msg error clientId :{} can‘t find hall server", clientId);
			}
		} else if ((msgId & MessageId.ROOM_TYPE) != 0) {
			clientId = gateClient.getRoomId();
			if (clientId != 0) {
				serverClient = serverManager.getServerClient(ServerType.Room, clientId);
			} else {
				serverClient = serverManager.getServerClient(ServerType.Room);
				gateClient.setRoomId((int) serverClient.getServerId());
			}
			if (serverClient != null) {
				serverClient.sendMessage(tcpMessage);
				return true;
			} else {
				logger.error("transform room msg error clientId :{} can‘t find room server", clientId);
			}
		}
		logger.error("[error msg transferMessage to server msgId:{}]", msgId);
		return false;
	}
}