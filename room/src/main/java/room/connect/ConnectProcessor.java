package room.connect;

import java.util.HashMap;
import java.util.Map;

import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;

public class ConnectProcessor {

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> false;
	public final static Parser PARSER = HandleTypeRegister::parseMessage;
	private final static Map<Integer, Handler> handlers = new HashMap<>();
	public final static Handlers HANDLERS = handlers::get;

	public static void init() {
		HandleTypeRegister.initFactory(ConnectProcessor.class, handlers);
	}

}
