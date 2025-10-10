package game.connect;

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
	public final static Parser PARSER = HandleTypeRegister::parseMessage;
	private final static Map<Integer, Handler> MAP = new HashMap<>();
	public final static Handlers HANDLERS = MAP::get;

	public static void init() {
		//绑定客户端消息处理
		HandleTypeRegister.bindClassPackageProcess(ConnectProcessor.class, MAP);
	}
}
