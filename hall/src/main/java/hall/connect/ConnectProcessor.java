package hall.connect;

import java.util.HashMap;
import java.util.Map;

import msg.registor.message.CMsg;
import msg.registor.enums.MessageTrans;
import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;

public class ConnectProcessor {

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	private final static Map<Integer, Handler> handlers = new HashMap<>();

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parserMessage(id, bytes, TRANS_MAP);

	public static void init() {
		HandleTypeRegister.bindClassProcess(ConnectProcessor.class, handlers);

		HandleTypeRegister.bindTransMap(CMsg.class, TRANS_MAP, MessageTrans.HallClient);
	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> false;

}
