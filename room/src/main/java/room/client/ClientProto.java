package room.client;

import java.util.HashMap;
import java.util.Map;

import msg.RoomMessageId;
import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import room.Room;
import utils.StringConst;

public class ClientProto {

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	private final static Map<Integer, Handler> handlers = new HashMap<>();

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parserMessage(id, bytes, TRANS_MAP);

	static {

		HandleTypeRegister.bindProcess(Room.class, handlers, "client,connect,model,manager");
		HandleTypeRegister.bindProcess(StringConst.HEAR_PACKAGE, handlers);

		HandleTypeRegister.bindTransMap(RoomMessageId.class, TRANS_MAP);
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer TRANSFER = (hallClient, tcpMessage) -> false;
}
