package room.client;

import java.util.HashMap;
import java.util.Map;

import msg.MessageId;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import room.handel.HeartHandler;
import room.handel.NotBreakHandler;
import room.handel.ReqRegisterHandler;
import room.handel.ReqRoomListHandler;

public class ClientProto {
	private final static Logger logger = LoggerFactory.getLogger(ClientProto.class);

	public final static Parser PARSER = (id, bytes) -> {

		switch (id) {
			case MessageId.REQ_REGISTER:
				return ModelProto.ReqRegister.parseFrom(bytes);
			case MessageId.HEART:
				return ModelProto.ReqHeart.parseFrom(bytes);
			case MessageId.NOT_BREAK:
				return ModelProto.NotBreak.parseFrom(bytes);
			default:
				return parserMessage(id, bytes);
		}
	};

	/**
	 * 消息转化
	 */
	private static com.google.protobuf.Message parserMessage(int id, byte[] bytes) {
		MessageId.RoomMsg roomMsg = MessageId.RoomMsg.get(id);
		if (roomMsg != null) {
			Class className = roomMsg.getClassName();
			try {
				return (com.google.protobuf.Message) MessageId.getMessageObject(className, bytes);
			} catch (Exception e) {
				logger.error("parse message error messageId :{} className:{}", id, className.getSimpleName());
			}
		}
		return null;
	}


	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(MessageId.HEART, HeartHandler.getInstance());
		handlers.put(MessageId.REQ_REGISTER, ReqRegisterHandler.getInstance());
		handlers.put(MessageId.NOT_BREAK, NotBreakHandler.getInstance());
		handlers.put(MessageId.RoomMsg.REQ_ROOM_LIST.getId(), ReqRoomListHandler.getInstance());
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer TRANSFER = (hallClient, tcpMessage) -> false;
}
