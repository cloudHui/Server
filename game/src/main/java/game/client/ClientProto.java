package game.client;

import java.util.HashMap;
import java.util.Map;

import game.handel.client.ReqEnterTableHandler;
import game.handel.server.HeartHandler;
import game.handel.server.NotBreakHandler;
import game.handel.server.ReqRegisterHandler;
import msg.Message;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

/**
 * 处理gate转发消息处理
 */
public class ClientProto {
	public final static Transfer<GameClient, TCPMessage> TRANSFER = (gameClient, tcpMessage) -> false;
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
	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(Message.HEART, HeartHandler.getInstance());
		handlers.put(Message.REQ_REGISTER, ReqRegisterHandler.getInstance());
		handlers.put(Message.NOT_BREAK, NotBreakHandler.getInstance());


		handlers.put(Message.GameMsg.REQ_ENTER_TABLE.getId(), ReqEnterTableHandler.getInstance());


	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 消息转化
	 */
	private static com.google.protobuf.Message parserMessage(int id, byte[] bytes) {
		Message.GameMsg gameMsg = Message.GameMsg.get(id);
		if (gameMsg != null) {
			Class className = gameMsg.getClassName();
			try {
				return (com.google.protobuf.Message) Message.getMessageObject(className, bytes);
			} catch (Exception e) {
				logger.error("parse message error messageId :{} className:{}", id, className.getSimpleName());
			}
		}
		return null;
	}
}
