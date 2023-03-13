package test.connect;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import msg.MessageHandel;
import net.connect.TCPConnect;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.handel.AckLoginHandler;
import utils.handel.HeartAckHandler;

/**
 * 与 gate 消息处理
 */
public class ConnectProcessor {
	private final static Logger logger = LoggerFactory.getLogger(ConnectProcessor.class);


	public final static Parser PARSER = ConnectProcessor::parserMessage;

	/**
	 * 消息转化
	 */
	private static Message parserMessage(int id, byte[] bytes) {
		MessageHandel.HallMsg hallMsg = MessageHandel.HallMsg.get(id);
		if (hallMsg != null) {
			Class className = hallMsg.getClassName();
			try {
				return (Message) MessageHandel.getMessageObject(className, bytes);
			} catch (Exception e) {
				logger.error("parse message error messageId :{} className:{}", id, className.getSimpleName());
			}
		}
		return null;
	}

	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(MessageHandel.HallMsg.ACK_LOGIN.getId(), AckLoginHandler.getInstance());
	}

	public final static Handlers HANDLERS = handlers::get;

	/**
	 * 转发消息接口
	 */
	public final static Transfer<TCPConnect, TCPMessage> TRANSFER = (tcpConnect, tcpMessage) -> false;

}
