package game.client;

import java.util.HashMap;
import java.util.Map;

import game.Game;
import msg.GameMessageId;
import msg.MessageId;
import msg.ServerType;
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
		HandleTypeRegister.bindProcess(Game.class, HANDLERS, "client");
		HandleTypeRegister.bindProcess(StringConst.HEAR_PACKAGE, HANDLERS);

		HandleTypeRegister.bindTransMap(GameMessageId.class, TRANS_MAP, ServerType.Game);
		HandleTypeRegister.bindTransMap(MessageId.class, TRANS_MAP, ServerType.Game);
	}

	public final static Handlers GET = HANDLERS::get;
}
