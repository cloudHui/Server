package game.client;

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

/**
 * 处理gate转发消息处理
 */
public class ClientProto {

	private final static Map<Integer, Handler> HANDLER_MAP = new HashMap<>();

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	public final static Transfer TRANSFER = (gameClient, tcpMessage) -> false;

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parseMessage(id, bytes, TRANS_MAP);

	public final static Handlers HANDLERS = HANDLER_MAP::get;

	public static void init() {
		//绑定专用服务器消息处理
		HandleTypeRegister.bindClassPackageProcess(ClientProto.class, HANDLER_MAP, TRANS_MAP);
		//绑定通用服务器消息处理
		HandleTypeRegister.bindDefaultPackageProcess(HANDLER_MAP, TRANS_MAP);

		//绑定game服务器消息解析处理
		HandleTypeRegister.bindTransMap(GMsg.class, TRANS_MAP, MessageTrans.GameServer);
		//绑定message服务器消息解析处理
		HandleTypeRegister.bindTransMap(CMsg.class, TRANS_MAP, MessageTrans.GameServer);
	}
}
