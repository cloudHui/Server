package game.client;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import game.handel.client.ReqEnterTableHandler;
import game.handel.server.ServerHandel;
import msg.MessageHandel;
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
	private final static Logger logger = LoggerFactory.getLogger(ClientProto.class);

	public final static Parser PARSER = (id, bytes) -> {
		switch (id) {
			case MessageHandel.REQ_REGISTER:
				return ModelProto.ReqRegister.parseFrom(bytes);
			case MessageHandel.HEART:
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
		MessageHandel.GameMsg gameMsg = MessageHandel.GameMsg.get(id);
		if (gameMsg != null) {
			Class className = gameMsg.getClassName();
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
		handlers.put(MessageHandel.HEART, ServerHandel.HEART_HANDLER);
		handlers.put(MessageHandel.REQ_REGISTER, ServerHandel.REGISTER_HANDLER);
		handlers.put(MessageHandel.NOT_BREAK, ServerHandel.NOT_BREAK_HANDLER);


		handlers.put(MessageHandel.GameMsg.ENTER_TABLE_REQ.getId(), ReqEnterTableHandler.getInstance());


	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer<GameClient, TCPMessage> TRANSFER = (gameClient, tcpMessage) -> false;
}
