package game.connect;

import java.util.HashMap;
import java.util.Map;

import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import proto.ModelProto;
import utils.handel.HeartAckHandler;

import static msg.MessageId.ACK_REGISTER;
import static msg.MessageId.HEART_ACK;

/**
 * 与center 消息处理
 */
public class ConnectProcessor {
	public final static Parser PARSER = (id, bytes) -> {
		switch (id) {
			case HEART_ACK:
				return ModelProto.AckHeart.parseFrom(bytes);
			case ACK_REGISTER:
				return ModelProto.AckRegister.parseFrom(bytes);
		}
		return null;
	};
	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> false;
	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(HEART_ACK, HeartAckHandler.getInstance());
	}

	public final static Handlers HANDLERS = handlers::get;

}
