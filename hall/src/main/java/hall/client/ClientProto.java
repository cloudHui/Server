package hall.client;

import java.util.HashMap;
import java.util.Map;

import msg.registor.HandleTypeRegister;
import msg.registor.enums.MessageTrans;
import msg.registor.message.CMsg;
import msg.registor.message.HMsg;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;

public class ClientProto {

	private final static Map<Integer, Handler> handlers = new HashMap<>();

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parseMessage(id, bytes, TRANS_MAP);

	public static void init() {
		HandleTypeRegister.bindClassPackageProcess(ClientProto.class, handlers, TRANS_MAP);

		HandleTypeRegister.bindDefaultPackageProcess(handlers, TRANS_MAP);

		HandleTypeRegister.bindTransMap(HMsg.class, TRANS_MAP, MessageTrans.HallServer);
		HandleTypeRegister.bindTransMap(CMsg.class, TRANS_MAP, MessageTrans.HallServer);
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer TRANSFER = (hallClient, tcpMessage) -> false;
}
