package robot.connect;

import java.util.HashMap;
import java.util.Map;

import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;

/**
 * 与center 消息处理
 */
public class ConnectProcessor {

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> false;
	private final static Map<Integer, Handler> MAP = new HashMap<>();
	/**
	 * 消息处理器获取
	 */
	public final static Handlers HANDLERS = MAP::get;
	/**
	 * 消息转换  bytes to Message(MessageLite)
	 */
	public final static Parser PARSER = HandleTypeRegister::parseMessage;

	public static void init() {
		HandleTypeRegister.initFactory(ConnectProcessor.class, MAP);
	}
}
