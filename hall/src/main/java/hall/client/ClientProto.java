package hall.client;

import java.util.HashMap;
import java.util.Map;

import hall.handel.ReqLoginHandler;
import hall.handel.ReqRoomListHandler;
import hall.handel.server.HeartHandler;
import hall.handel.server.NotBreakHandler;
import hall.handel.server.ReqRegisterHandler;
import msg.Message;
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
			case Message.REQ_REGISTER:
				return ModelProto.ReqRegister.parseFrom(bytes);
			case Message.HEART:
				return ModelProto.ReqHeart.parseFrom(bytes);
			case Message.NOT_BREAK:
				return ModelProto.NotBreak.parseFrom(bytes);
			default:
				return parserMessage(id, bytes);
		}
	};

	/**
	 * 消息转化
	 */
	private static com.google.protobuf.Message parserMessage(int id, byte[] bytes) {
		Message.HallMsg hallMsg = Message.HallMsg.get(id);
		if (hallMsg != null) {
			Class className = hallMsg.getClassName();
			try {
				return (com.google.protobuf.Message) Message.getMessageObject(className, bytes);
			} catch (Exception e) {
				logger.error("parse message error messageId :{} className:{}", id, className.getSimpleName());
			}
		}
		return null;
	}


	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(Message.HEART, HeartHandler.getInstance());
		handlers.put(Message.REQ_REGISTER, ReqRegisterHandler.getInstance());
		handlers.put(Message.NOT_BREAK, NotBreakHandler.getInstance());
		handlers.put(Message.HallMsg.REQ_LOGIN.getId(), ReqLoginHandler.getInstance());
		handlers.put(Message.HallMsg.REQ_ROOM_LIST.getId(), ReqRoomListHandler.getInstance());
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer<HallClient, TCPMessage> TRANSFER = (hallClient, tcpMessage) -> false;
}
