package gate.connect;

import java.util.HashMap;
import java.util.Map;

import msg.registor.HandleTypeRegister;
import msg.registor.enums.MessageTrans;
import msg.registor.message.CMsg;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;

public class ConnectProcessor {

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parseMessage(id, bytes, TRANS_MAP);

	private final static Map<Integer, Handler> handlers = new HashMap<>();

	public static void init() {
		HandleTypeRegister.bindClassPackageProcess(ConnectProcessor.class, handlers, TRANS_MAP);
		HandleTypeRegister.bindTransMap(CMsg.class, TRANS_MAP, MessageTrans.GateClient);
	}

	public final static Handlers HANDLERS = handlers::get;
}
