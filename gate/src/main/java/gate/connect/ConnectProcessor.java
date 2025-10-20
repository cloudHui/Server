package gate.connect;

import java.util.HashMap;
import java.util.Map;

import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;

public class ConnectProcessor {

	private final static Map<Integer, Handler> handlers = new HashMap<>();

	public final static Parser PARSER = HandleTypeRegister::parseMessage;

	public final static Handlers HANDLERS = handlers::get;

	public final static Transfer TRANSFER = (connect, tcpMessage) -> {
		//Todo 有些消息直接转发给客户端 直接通知的
		return false;
	};

	public static void init() {
		HandleTypeRegister.initFactory(ConnectProcessor.class, handlers);
	}
}
