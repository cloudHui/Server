package hall.client;

import java.util.HashMap;
import java.util.Map;

import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;

public class ClientProto {

	public final static Transfer TRANSFER = (hallClient, tcpMessage) -> false;
	private final static Map<Integer, Handler> handlers = new HashMap<>();
	public final static Handlers HANDLERS = handlers::get;
	public final static Parser PARSER = HandleTypeRegister::parseMessage;

	public static void init() {
		HandleTypeRegister.initFactory(ClientProto.class, handlers);

		HandleTypeRegister.initFactory(handlers);
	}
}
