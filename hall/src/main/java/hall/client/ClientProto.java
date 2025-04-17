package hall.client;

import java.util.HashMap;
import java.util.Map;

import hall.Hall;
import msg.HallMessageId;
import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import utils.StringConst;

public class ClientProto {

	private final static Map<Integer, Handler> handlers = new HashMap<>();

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parserMessage(id, bytes, TRANS_MAP);

	static {
		HandleTypeRegister.bindProcess(Hall.class, handlers, "client, connect,db");

		HandleTypeRegister.bindProcess(StringConst.HEAR_PACKAGE, handlers);

		HandleTypeRegister.bindTransMap(HallMessageId.class, TRANS_MAP);
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer TRANSFER = (hallClient, tcpMessage) -> false;
}
