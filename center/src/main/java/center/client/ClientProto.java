package center.client;

import java.util.HashMap;
import java.util.Map;

import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;

public class ClientProto {

	public final static Transfer TRANSFER = (routerClient, tcpMessage) -> false;
	private final static Map<Integer, Handler> MAP = new HashMap<>();
	public final static Handlers HANDLERS = MAP::get;
	public final static Parser PARSER = HandleTypeRegister::parseMessage;

	public static void init() {
		//绑定自带服务器处理
		HandleTypeRegister.initFactory(ClientProto.class, MAP);
		//绑定通用服务器处理
		HandleTypeRegister.initFactory(MAP);
	}
}
