package center.client;

import java.util.HashMap;
import java.util.Map;

import msg.registor.HandleTypeRegister;
import msg.registor.enums.MessageTrans;
import msg.registor.message.CMsg;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;

public class ClientProto {

	private final static Map<Integer, Handler> MAP = new HashMap<>();

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	public static void init() {
		//绑定自带服务器处理
		HandleTypeRegister.bindClassProcess(ClientProto.class, MAP);
		//绑定通用服务器处理
		HandleTypeRegister.bindPackageProcess(MAP);
		//绑定通用消息转换处理
		HandleTypeRegister.bindTransMap(CMsg.class, TRANS_MAP, MessageTrans.CenterServer);
	}

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parserMessage(id, bytes, TRANS_MAP);

	public final static Handlers HANDLERS = MAP::get;


	public final static Transfer TRANSFER = (routerClient, tcpMessage) -> false;
}
