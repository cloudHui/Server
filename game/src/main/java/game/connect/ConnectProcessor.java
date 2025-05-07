package game.connect;

import java.util.HashMap;
import java.util.Map;

import msg.registor.message.CMsg;
import msg.registor.enums.MessageTrans;
import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;

/**
 * 与center 消息处理
 */
public class ConnectProcessor {
	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	private final static Map<Integer, Handler> MAP = new HashMap<>();

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parserMessage(id, bytes, TRANS_MAP);

	public static void init() {
		//绑定客户端消息处理
		HandleTypeRegister.bindClassProcess(ConnectProcessor.class, MAP);
		//绑定通用服务器消息解析处理
		HandleTypeRegister.bindTransMap(CMsg.class, TRANS_MAP, MessageTrans.GameClient);
	}

	public final static Handlers HANDLERS = MAP::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> false;
}
