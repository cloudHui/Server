package robot.connect;

import java.util.HashMap;
import java.util.Map;

import msg.GameMessageId;
import msg.HallMessageId;
import msg.MessageTrans;
import msg.RoomMessageId;
import msg.registor.HandleTypeRegister;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;

/**
 * 与center 消息处理
 */
public class ConnectProcessor {

	private final static Map<Integer, Handler> MAP = new HashMap<>();

	private final static Map<Integer, Class<?>> TRANS_MAP = new HashMap<>();

	public static void init() {
		HandleTypeRegister.bindClassProcess(ConnectProcessor.class, MAP);

		HandleTypeRegister.bindTransMap(HallMessageId.class, TRANS_MAP, MessageTrans.RobotClient);
		HandleTypeRegister.bindTransMap(RoomMessageId.class, TRANS_MAP, MessageTrans.RobotClient);
		HandleTypeRegister.bindTransMap(GameMessageId.class, TRANS_MAP, MessageTrans.RobotClient);
	}


	/**
	 * 消息转换  bytes to Message(MessageLite)
	 */
	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parserMessage(id, bytes, TRANS_MAP);

	/**
	 * 消息处理器获取
	 */
	public final static Handlers HANDLERS = MAP::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> false;
}
