package game.connect;

import java.util.HashMap;
import java.util.Map;

import msg.MessageId;
import msg.ServerType;
import msg.registor.HandleTypeRegister;
import net.message.Parser;
import net.message.Transfer;

/**
 * 与center 消息处理
 */
public class ConnectProcessor {
	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parserMessage(id, bytes, TRANS_MAP);

	static {
		HandleTypeRegister.bindTransMap(MessageId.class, TRANS_MAP, ServerType.Game);
	}

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> false;
}
