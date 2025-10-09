package room.connect;

import java.util.HashMap;
import java.util.Map;

import msg.registor.HandleTypeRegister;
import msg.registor.enums.MessageTrans;
import msg.registor.message.CMsg;
import msg.registor.message.GMsg;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;

public class ConnectProcessor {

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parseMessage(id, bytes, TRANS_MAP);

	private final static Map<Integer, Handler> handlers = new HashMap<>();

	public static void init() {
		HandleTypeRegister.bindClassPackageProcess(ConnectProcessor.class, handlers, TRANS_MAP);

		HandleTypeRegister.bindTransMap(CMsg.class, TRANS_MAP, MessageTrans.RoomClient);
		HandleTypeRegister.bindTransMap(GMsg.class, TRANS_MAP, MessageTrans.RoomClient);
	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> false;

}
