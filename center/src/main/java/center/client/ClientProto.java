package center.client;

import java.util.HashMap;
import java.util.Map;

import msg.MessageId;
import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import utils.StringConst;

public class ClientProto {

	private final static Map<Integer, Handler> MAP = new HashMap<>();

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	public static void init() {
		//绑定自带服务器处理
		HandleTypeRegister.bindClassProcess(ClientProto.class, MAP);
		//绑定通用服务器处理
		HandleTypeRegister.bindPackageProcess(StringConst.HEAR_PACKAGE, MAP);
		//绑定通用消息转换处理
		HandleTypeRegister.bindCommonTransMap(ClientProto.class, TRANS_MAP, MessageId.SERVER);
	}

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parserMessage(id, bytes, TRANS_MAP);

	public final static Handlers HANDLERS = MAP::get;


	public final static Transfer TRANSFER = (routerClient, tcpMessage) -> false;
}
