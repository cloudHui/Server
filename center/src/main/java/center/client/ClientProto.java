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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;

public class ClientProto {

	private static final Logger logger = LoggerFactory.getLogger(ClientProto.class);

	public final static Parser PARSER = (id, bytes) -> {
		switch (id) {
			case MessageHandel.HEART_REQ:
				return ModelProto.ReqHeart.parseFrom(bytes);
			case MessageHandel.REGISTER:
				return ModelProto.ReqRegister.parseFrom(bytes);
			case MessageHandel.SERVER_REQ:
				return ModelProto.ReqRegister.parseFrom(bytes);
			default:
				return null;
		}
	};

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(MessageHandel.HEART_REQ, HeartHandler.getInstance());
		handlers.put(MessageHandel.REGISTER, ReqRegisterHandler.getInstance());
		handlers.put(MessageHandel.SERVER_REQ, ReqServerInfoHandler.getInstance());
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer<CenterClient, TCPMessage> TRANSFER = (routerClient, tcpMessage) -> false;
}
