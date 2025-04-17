package room.connect;

import java.util.HashMap;
import java.util.Map;

import msg.MessageId;
import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import room.Room;

public class ConnectProcessor {

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parserMessage(id, bytes, TRANS_MAP);

	private final static Map<Integer, Handler> handlers = new HashMap<>();

	static {
		HandleTypeRegister.bindProcess(Room.class, handlers, "client,connect,model,manager");

		HandleTypeRegister.bindTransMap(MessageId.class, TRANS_MAP, MessageId.CLIENT);
	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> false;

}
