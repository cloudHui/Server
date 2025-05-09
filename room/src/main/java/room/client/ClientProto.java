package room.client;

import java.util.HashMap;
import java.util.Map;

import msg.registor.HandleTypeRegister;
import msg.registor.enums.MessageTrans;
import msg.registor.message.CMsg;
import msg.registor.message.RMsg;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;

public class ClientProto {

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	private final static Map<Integer, Handler> handlers = new HashMap<>();

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parserMessage(id, bytes, TRANS_MAP);

	public static void init() {

		HandleTypeRegister.bindClassProcess(ClientProto.class, handlers);
		HandleTypeRegister.bindPackageProcess(handlers);

		HandleTypeRegister.bindTransMap(RMsg.class, TRANS_MAP, MessageTrans.RoomServer);
		HandleTypeRegister.bindTransMap(CMsg.class, TRANS_MAP, MessageTrans.RoomServer);
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer TRANSFER = (hallClient, tcpMessage) -> false;
}
