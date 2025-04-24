package hall.client;

import java.util.HashMap;
import java.util.Map;

import msg.HallMessageId;
import msg.MessageId;
import msg.MessageTrans;
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

	public static void init() {
		HandleTypeRegister.bindClassProcess(ClientProto.class, handlers);

		HandleTypeRegister.bindPackageProcess(StringConst.HEAR_PACKAGE, handlers);

		HandleTypeRegister.bindTransMap(HallMessageId.class, TRANS_MAP, MessageTrans.HallServer);
		HandleTypeRegister.bindTransMap(MessageId.class, TRANS_MAP, MessageTrans.HallServer);
	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer TRANSFER = (hallClient, tcpMessage) -> false;
}
