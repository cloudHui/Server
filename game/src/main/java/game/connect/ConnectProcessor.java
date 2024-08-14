package game.connect;

import java.util.HashMap;
import java.util.Map;

import msg.MessageId;
import net.connect.TCPConnect;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import proto.ModelProto;
import utils.handel.HeartAckHandler;

/**
 * 与center 消息处理
 */
public class ConnectProcessor {
	public final static Parser PARSER = (id, bytes) -> {
		if (id == MessageId.HEART_ACK) {
			return ModelProto.AckHeart.parseFrom(bytes);
		}
		return null;
	};
	/**
	 * 转发消息接口
	 */
	public final static Transfer<TCPConnect, TCPMessage> TRANSFER = (tcpConnect, tcpMessage) -> false;
	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(MessageId.HEART_ACK, HeartAckHandler.getInstance());
	}

	public final static Handlers HANDLERS = handlers::get;

}
