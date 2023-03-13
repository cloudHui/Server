package gate.connect;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import gate.client.ClientProto;
import gate.handel.server.AckServerInfoHandel;
import gate.handel.server.RegisterNoticeHandler;
import msg.MessageHandel;
import net.client.Sender;
import net.client.handler.ClientHandler;
import net.connect.ConnectHandler;
import net.connect.TCPConnect;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.handel.HeartAckHandler;

public class ConnectProcessor {

	private final static Logger logger = LoggerFactory.getLogger(ClientProto.class);


	public final static Parser PARSER = (id, bytes) -> {
		switch (id) {
			case MessageHandel.HEART_ACK:
				return ModelProto.AckHeart.parseFrom(bytes);
			case MessageHandel.REQ_REGISTER:
				return ModelProto.ReqRegister.parseFrom(bytes);
			case MessageHandel.ACK_SERVER:
				return ModelProto.AckServerInfo.parseFrom(bytes);
			case MessageHandel.ACK_REGISTER:
				return ModelProto.AckRegister.parseFrom(bytes);
			case MessageHandel.REGISTER_NOTICE:
				return ModelProto.NotRegisterInfo.parseFrom(bytes);
			default: {
				return null;
			}
		}
	};

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(MessageHandel.REGISTER_NOTICE, RegisterNoticeHandler.getInstance());
		handlers.put(MessageHandel.HEART_ACK, HeartAckHandler.getInstance());
		handlers.put(MessageHandel.ACK_SERVER, AckServerInfoHandel.getInstance());
	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer<TCPConnect, TCPMessage> TRANSFER = (tcpConnect, tcpMessage) -> {
		int msgId = tcpMessage.getMessageId();
		if (msgId > MessageHandel.BASE_ID_INDEX) {
			//直接转发给客户端的
			return transferMsg(tcpMessage.getMapId(), tcpMessage);
		}
		return false;
	};

	public static boolean transferMsg(long connectId, TCPMessage msg) {
		return transferMsg(connectId, msg, null);
	}

	public static boolean transferMsg(long connectId, TCPMessage msg, Message innerMsg) {
		Sender sender = ClientHandler.getClient(connectId);
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
