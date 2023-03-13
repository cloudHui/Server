package gate.connect;

import java.util.HashMap;
import java.util.Map;

import gate.handel.server.AckServerInfoHandel;
import gate.handel.server.RegisterNoticeHandler;
import msg.MessageHandel;
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
			case MessageHandel.HEART_ACK:
				return ModelProto.AckHeart.parseFrom(bytes);
			case MessageHandel.REQ_REGISTER:
				return ModelProto.ReqRegister.parseFrom(bytes);
			case MessageHandel.ACK_SERVER:
				return ModelProto.AckServerInfo.parseFrom(bytes);
			case MessageHandel.ACK_REGISTER:
				return ModelProto.AckRegister.parseFrom(bytes);
			case MessageHandel.REGISTER_NOTICE:
				return ModelProto.NotRegisterInfo.parseFrom(bytes);
			default: {
				return null;
			}
		}
	};

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(MessageHandel.REGISTER_NOTICE, RegisterNoticeHandler.getInstance());
		handlers.put(MessageHandel.HEART_ACK, HeartAckHandler.getInstance());
		handlers.put(MessageHandel.ACK_SERVER, AckServerInfoHandel.getInstance());
	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer<TCPConnect, TCPMessage> TRANSFER = (tcpConnect, tcpMessage) -> false;

}
