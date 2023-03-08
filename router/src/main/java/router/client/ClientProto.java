package router.client;

import java.util.HashMap;
import java.util.Map;

import msg.MsgId;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import proto.ModelProto;
import router.handle.HeartHandler;
import router.handle.RegisterEventHandler;

public class ClientProto {

	public final static Parser PARSER = (id, bytes) -> {
		switch (id) {
			case MsgId.REGISTER:
				return ModelProto.RegisterNotice.parseFrom(bytes);
			case MsgId.HEART_REQ:
				return ModelProto.ReqHeart.parseFrom(bytes);
			default: {
				return null;
			}
		}
	};


	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(MsgId.HEART_REQ, HeartHandler.getInstance());
		handlers.put(MsgId.REGISTER, RegisterEventHandler.getInstance());
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer<RouterClient, TCPMessage> TRANSFER = (routerClient, tcpMessage) -> false;
}
