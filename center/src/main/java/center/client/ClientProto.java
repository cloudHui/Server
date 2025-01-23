package center.client;

import java.util.HashMap;
import java.util.Map;

import center.handel.ReqRegisterHandler;
import center.handel.ReqServerInfoHandler;
import msg.MessageId;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import proto.ModelProto;
import utils.handel.HeartHandler;

public class ClientProto {

	public final static Parser PARSER = (id, bytes) -> {
		switch (id) {
			case MessageId.HEART:
				return ModelProto.ReqHeart.parseFrom(bytes);
			case MessageId.REQ_REGISTER:
				return ModelProto.ReqRegister.parseFrom(bytes);
			case MessageId.REQ_SERVER:
				return ModelProto.ReqServerInfo.parseFrom(bytes);
			default:
				return null;
		}
	};

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(MessageId.HEART, HeartHandler.getInstance());
		handlers.put(MessageId.REQ_REGISTER, ReqRegisterHandler.getInstance());
		handlers.put(MessageId.REQ_SERVER, ReqServerInfoHandler.getInstance());
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer TRANSFER = (routerClient, tcpMessage) -> false;
}
