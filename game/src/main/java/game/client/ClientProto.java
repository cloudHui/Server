package game.client;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import game.Game;
import game.msg.GameMessageId;
import msg.MessageId;
import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.StringConst;

/**
 * 处理gate转发消息处理
 */
public class ClientProto {

	public final static Transfer TRANSFER = (gameClient, tcpMessage) -> false;

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
	private final static Map<Integer, Handler> HANDLERS = new HashMap<>();

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();


	public static void init() {
		HandleTypeRegister.bindProcess(Game.class, HANDLERS, "client");
		HandleTypeRegister.bindProcess(StringConst.HEAR_PACKAGE, HANDLERS);

		HandleTypeRegister.bindTransMap(GameMessageId.class, TRANS_MAP);
	}

	public final static Handlers GET = HANDLERS::get;

	/**
	 * 消息转化
	 */
	private static Message parserMessage(int id, byte[] bytes) {
		Class<?> gameMsg = TRANS_MAP.get(id);
		if (gameMsg != null) {
			try {
				return (Message) MessageId.getMessageObject((Class<MessageLite>) gameMsg, bytes);
			} catch (Exception e) {
				logger.error("[parse message error messageId :{} className:{}]", id, gameMsg.getSimpleName());
			}
		}
		return null;
	}
}
