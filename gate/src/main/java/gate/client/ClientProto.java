package gate.client;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import gate.Gate;
import msg.MessageHandel;
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
	private static Message parserMessage(int id, byte[] bytes) {
		MessageHandel.GateMsg gateMsg = MessageHandel.GateMsg.get(id);
		if (gateMsg != null) {
			Class className = gateMsg.getClassName();
			try {
				return (Message) MessageHandel.getMessageObject(className, bytes);
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
	public final static Transfer<GateClient, TCPMessage> TRANSFER = (gateClient, tcpMessage) -> {
		int msgId = tcpMessage.getMessageId();
		if (msgId > MessageHandel.BASE_ID_INDEX) {
			return transferMessage(gateClient, tcpMessage, msgId);
		}
		return false;
	};


	/**
	 * 消息转发到服务器
	 */
	private static boolean transferMessage(GateClient gateClient, TCPMessage tcpMessage, int msgId) {
		//奇数消息是发给服务的
		if ((msgId & 1) != 0) {
			ServerManager serverManager = Gate.getInstance().getServerManager();
			tcpMessage.setMapId((int) gateClient.getId());
			int clientId;
			TCPConnect serverClient;
			if ((msgId & MessageHandel.GAME_TYPE) != 0) {
				clientId = gateClient.getGameId();
				if (clientId != 0) {
					serverClient = serverManager.getServerClient(ServerType.Game, clientId);
				} else {
					serverClient = serverManager.getServerClient(ServerType.Game);
					gateClient.setGameId((int) serverClient.getId());
				}
				if (serverClient != null) {
					serverClient.sendMessage(tcpMessage);
					return true;
				}
			} else if ((msgId & MessageHandel.HALL_TYPE) != 0) {
				clientId = gateClient.getHallId();
				if (clientId != 0) {
					serverClient = serverManager.getServerClient(ServerType.Hall, clientId);
				} else {
					serverClient = serverManager.getServerClient(ServerType.Hall);
					gateClient.setHallId((int) serverClient.getId());
				}
				if (serverClient != null) {
					serverClient.sendMessage(tcpMessage);
					return true;
				}
			}
		}
		logger.error("[error msg transferMessage to server msgId:{}]", msgId);
		return false;
	}
}