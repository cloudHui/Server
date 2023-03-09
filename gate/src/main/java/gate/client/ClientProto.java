package gate.client;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import gate.handel.AckServerInfoHandel;
import gate.handel.HeartHandler;
import gate.handel.RegisterNoticeHandler;
import msg.MessageHandel;
import net.client.Sender;
import net.connect.ConnectHandler;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

public class ClientProto {
	private final static Logger logger = LoggerFactory.getLogger(ClientProto.class);

	public final static Parser PARSER = (id, bytes) -> {

		switch (id) {
			case MessageHandel.HEART_REQ:
				return ModelProto.ReqHeart.parseFrom(bytes);
			case MessageHandel.REGISTER:
				return ModelProto.ReqRegister.parseFrom(bytes);
			default:
				return parserMessage(id, bytes);
		}
	};

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
		} else {
			MessageHandel.CenterMsg centerMsg = MessageHandel.CenterMsg.get(id);
			if (centerMsg != null) {
				Class className = centerMsg.getClassName();
				try {
					return (Message) MessageHandel.getMessageObject(className, bytes);
				} catch (Exception e) {
					logger.error("parse message error messageId :{} className:{}", id, className.getSimpleName());
				}
			}
		}
		return null;
	}

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(MessageHandel.REGISTER_NOTICE, RegisterNoticeHandler.getInstance());
		handlers.put(MessageHandel.HEART_REQ, HeartHandler.getInstance());
		handlers.put(MessageHandel.CenterMsg.SERVER_ACK.getId(), AckServerInfoHandel.getInstance());


	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer<GateClient, TCPMessage> TRANSFER = (gateClient, tcpMessage) -> {
		//Todo  special  server back msg need fill gate client serverId
		int msgId = tcpMessage.getMessageId();
		if (msgId % 2 == 0) {
			msgId /= MessageHandel.BASE_ID_INDEX;
			switch (msgId) {
				case MessageHandel.GAME_TYPE:
					break;
				case MessageHandel.GATE_TYPE:
					break;
				case MessageHandel.HALL_TYPE:
					break;
				default:
					logger.error("[error msg head:{} msgId:{}]", msgId, tcpMessage.getMessageId());
					break;
			}
		} else {
			return transferMsg(gateClient.getId(), tcpMessage);
		}
		return false;
	};

	public static boolean transferMsg(long connectId, TCPMessage msg) {
		return transferMsg(connectId, msg, null);
	}

	public static boolean transferMsg(long connectId, TCPMessage msg, Message innerMsg) {
		Sender sender = ConnectHandler.getSender(connectId);
		if (null != sender) {
			if (null != innerMsg) {
				sender.sendMessage(replace(msg, innerMsg));
			} else {
				sender.sendMessage(msg);
			}
			return true;
		}

		logger.error("ERROR! failed for transfer message(connect:{} message id:{})", connectId, msg.getMessageId());
		return false;
	}

	public static TCPMessage replace(TCPMessage tcpMessage, Message msg) {
		tcpMessage.setMessage(msg.toByteArray());
		return tcpMessage;
	}
}
