package room.client;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import msg.MessageId;
import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import room.Room;
import utils.StringConst;

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
	private static Message parserMessage(int id, byte[] bytes) {
		MessageId.RoomMsg roomMsg = MessageId.RoomMsg.get(id);
		if (roomMsg != null) {
			Class<? extends MessageLite> className = roomMsg.getClassName();
			try {
				return (Message) MessageId.getMessageObject((Class<MessageLite>) className, bytes);
			} catch (Exception e) {
				logger.error("[parse message error messageId :{} className:{}]", id, className.getSimpleName());
			}
		}
		return null;
	}


	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		HandleTypeRegister.bindProcess(Room.class, handlers, "client, connect,db");
		HandleTypeRegister.bindProcess(StringConst.HEAR_PACKAGE, handlers);
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer TRANSFER = (hallClient, tcpMessage) -> false;
}
