package center.client;

import java.util.HashMap;
import java.util.Map;

import center.handel.ServerHandel;
import msg.Message;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import proto.ModelProto;

public class ClientProto {

	public final static Parser PARSER = (id, bytes) -> {
		switch (id) {
			case Message.HEART:
				return ModelProto.ReqHeart.parseFrom(bytes);
			case Message.REQ_REGISTER:
				return ModelProto.ReqRegister.parseFrom(bytes);
			case Message.REQ_SERVER:
				return ModelProto.ReqServerInfo.parseFrom(bytes);
			default:
				return null;
		}
	};

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(Message.HEART, ServerHandel.HEART_HANDLER);
		handlers.put(Message.REQ_REGISTER, ServerHandel.REGISTER_HANDLER);
		handlers.put(Message.REQ_SERVER, ServerHandel.SERVER_INFO_HANDLER);
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer<CenterClient, TCPMessage> TRANSFER = (routerClient, tcpMessage) -> false;
}
