package center.client;

import java.util.HashMap;
import java.util.Map;

import center.handel.HeartHandler;
import center.handel.ReqRegisterHandler;
import center.handel.ReqServerInfoHandler;
import msg.MessageHandel;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import proto.ModelProto;

public class ClientProto {

	public final static Parser PARSER = (id, bytes) -> {
		switch (id) {
			case MessageHandel.HEART:
				return ModelProto.ReqHeart.parseFrom(bytes);
			case MessageHandel.REQ_REGISTER:
				return ModelProto.ReqRegister.parseFrom(bytes);
			case MessageHandel.REQ_SERVER:
				return ModelProto.ReqServerInfo.parseFrom(bytes);
			default:
				return null;
		}
	};

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(MessageHandel.HEART, HeartHandler.getInstance());
		handlers.put(MessageHandel.REQ_REGISTER, ReqRegisterHandler.getInstance());
		handlers.put(MessageHandel.REQ_SERVER, ReqServerInfoHandler.getInstance());
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer<CenterClient, TCPMessage> TRANSFER = (routerClient, tcpMessage) -> false;
}
