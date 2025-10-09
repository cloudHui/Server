package robot.connect;

import java.util.HashMap;
import java.util.Map;

import msg.registor.message.CMsg;
import msg.registor.message.GMsg;
import msg.registor.message.HMsg;
import msg.registor.enums.MessageTrans;
import msg.registor.message.RMsg;
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
		HandleTypeRegister.bindClassPackageProcess(ConnectProcessor.class, MAP, TRANS_MAP);

		HandleTypeRegister.bindTransMap(CMsg.class, TRANS_MAP, MessageTrans.RobotClient);
		HandleTypeRegister.bindTransMap(HMsg.class, TRANS_MAP, MessageTrans.RobotClient);
		HandleTypeRegister.bindTransMap(RMsg.class, TRANS_MAP, MessageTrans.RobotClient);
		HandleTypeRegister.bindTransMap(GMsg.class, TRANS_MAP, MessageTrans.RobotClient);
	}


	/**
	 * 消息转换  bytes to Message(MessageLite)
	 */
	public final static Parser PARSER = (id, bytes) -> HandleTypeRegister.parseMessage(id, bytes, TRANS_MAP);

	/**
	 * 消息处理器获取
	 */
	public final static Handlers HANDLERS = MAP::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer TRANSFER = (tcpConnect, tcpMessage) -> false;
}
