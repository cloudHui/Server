package center.client;

import java.util.HashMap;
import java.util.Map;

import center.Center;
import msg.MessageId;
import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import proto.ModelProto;
import utils.StringConst;

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
		HandleTypeRegister.bindProcess(Center.class, handlers,"client");
		HandleTypeRegister.bindProcess(StringConst.HEAR_PACKAGE, handlers);
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer TRANSFER = (routerClient, tcpMessage) -> false;
}
