package room.connect;

import java.util.HashMap;
import java.util.Map;

import msg.Message;
import net.connect.TCPConnect;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import proto.ModelProto;
import utils.handel.HeartAckHandler;

public class ConnectProcessor {
	public final static Parser PARSER = (id, bytes) -> {
		switch (id) {
			case Message.HEART_ACK:
				return ModelProto.AckHeart.parseFrom(bytes);
			case Message.ACK_REGISTER:
				return ModelProto.AckRegister.parseFrom(bytes);
			default: {
				return null;
			}
		}
	};

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(Message.HEART_ACK, HeartAckHandler.getInstance());
	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer<TCPConnect, TCPMessage> TRANSFER = (tcpConnect, tcpMessage) -> false;

}
