package game.client;

import java.util.HashMap;
import java.util.Map;

import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;

/**
 * 处理gate转发消息处理
 */
public class ClientProto {

	public final static Transfer TRANSFER = (gameClient, tcpMessage) -> false;
	private final static Map<Integer, Handler> HANDLER_MAP = new HashMap<>();
	public final static Handlers HANDLERS = HANDLER_MAP::get;
	public final static Parser PARSER = HandleTypeRegister::parseMessage;

	public static void init() {
		//绑定专用服务器消息处理
		HandleTypeRegister.initFactory(ClientProto.class, HANDLER_MAP);
		//绑定通用服务器消息处理
		HandleTypeRegister.initFactory(HANDLER_MAP);
	}
}
