package game.client;

import java.util.HashMap;
import java.util.Map;

import msg.GameMessageId;
import msg.MessageId;
import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import utils.StringConst;

/**
 * 处理gate转发消息处理
 */
public class ClientProto {

	public final static Transfer TRANSFER = (gameClient, tcpMessage) -> false;

	private final static Map<Integer, Handler> HANDLERS = new HashMap<>();

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parserMessage(id, bytes, TRANS_MAP);

	public static void init() {
		//绑定专用服务器消息处理
		HandleTypeRegister.bindClassProcess(ClientProto.class, HANDLERS);
		//绑定通用服务器消息处理
		HandleTypeRegister.bindPackageProcess(StringConst.HEAR_PACKAGE, HANDLERS);

		//绑定专用服务器消息解析处理
		HandleTypeRegister.bindUniqTransMap(GameMessageId.class, TRANS_MAP);
		//绑定通用服务器消息解析处理
		HandleTypeRegister.bindCommonTransMap(ClientProto.class, TRANS_MAP, MessageId.SERVER);
	}

	public final static Handlers GET = HANDLERS::get;
}
