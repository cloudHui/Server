package test.connect;

import java.util.HashMap;
import java.util.Map;

import msg.Message;
import net.connect.TCPConnect;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.handel.ServerHandel;

/**
 * 与 gate 消息处理
 */
public class ConnectProcessor {
	private final static Logger logger = LoggerFactory.getLogger(ConnectProcessor.class);


	public final static Parser PARSER = ConnectProcessor::parserMessage;

	/**
	 * 消息转化
	 */
	private static com.google.protobuf.Message parserMessage(int id, byte[] bytes) {
		Message.HallMsg hallMsg = Message.HallMsg.get(id);
		if (hallMsg != null) {
			Class className = hallMsg.getClassName();
			try {
				return (com.google.protobuf.Message) Message.getMessageObject(className, bytes);
			} catch (Exception e) {
				logger.error("parse message error messageId :{} className:{}", id, className.getSimpleName());
			}
		} else {
			Message.GameMsg gameMsg = Message.GameMsg.get(id);
			if (gameMsg != null) {
				Class className = gameMsg.getClassName();
				try {
					return (com.google.protobuf.Message) Message.getMessageObject(className, bytes);
				} catch (Exception e) {
					logger.error("parse message error messageId :{} className:{}", id, className.getSimpleName());
				}
			}
		}
		return null;
	}

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(Message.HallMsg.ACK_LOGIN.getId(), ServerHandel.ACK_LOGIN_HANDLER);
		handlers.put(Message.GameMsg.ACK_ENTER_TABLE.getId(), ServerHandel.ACK_ENTER_TABLE_HANDLER);
	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer<TCPConnect, TCPMessage> TRANSFER = (tcpConnect, tcpMessage) -> false;

}
