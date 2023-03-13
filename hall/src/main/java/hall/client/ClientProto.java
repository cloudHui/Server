package hall.client;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import hall.handel.NotBreakHandler;
import hall.handel.ReqRegisterHandler;
import msg.MessageHandel;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.handel.HeartAckHandler;

public class ClientProto {
	private final static Logger logger = LoggerFactory.getLogger(ClientProto.class);

	public final static Parser PARSER = (id, bytes) -> {

		switch (id) {
			case MessageHandel.REQ_REGISTER:
				return ModelProto.ReqRegister.parseFrom(bytes);
			case MessageHandel.HEART_ACK:
				return ModelProto.ReqHeart.parseFrom(bytes);
			case MessageHandel.NOT_BREAK:
				return ModelProto.NotBreak.parseFrom(bytes);
			default:
				return parserMessage(id, bytes);
		}
	};

	/**
	 * 消息转化
	 */
	private static Message parserMessage(int id, byte[] bytes) {
		MessageHandel.HallMsg hallMsg = MessageHandel.HallMsg.get(id);
		if (hallMsg != null) {
			Class className = hallMsg.getClassName();
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
		handlers.put(MessageHandel.HEART_ACK, HeartAckHandler.getInstance());
		handlers.put(MessageHandel.REQ_REGISTER, ReqRegisterHandler.getInstance());
		handlers.put(MessageHandel.NOT_BREAK, NotBreakHandler.getInstance());
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer<HallClient, TCPMessage> TRANSFER = (hallClient, tcpMessage) -> false;
}
