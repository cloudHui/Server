package gate.connect;

import java.util.HashMap;
import java.util.Map;

import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;

public class ConnectProcessor {

	public final static Parser PARSER = HandleTypeRegister::parseMessage;

	private final static Map<Integer, Handler> handlers = new HashMap<>();
	public final static Handlers HANDLERS = handlers::get;

	public static void init() {
		HandleTypeRegister.bindClassPackageProcess(ConnectProcessor.class, handlers);
	}
}
