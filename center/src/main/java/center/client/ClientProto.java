package center.client;

import java.util.HashMap;
import java.util.Map;

import center.handle.HeartHandler;
import center.handle.RegisterEventHandler;
import center.handle.ReqServerInfoHandler;
import com.google.protobuf.Message;
import msg.MessageHandel;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientProto {

	private static final Logger logger = LoggerFactory.getLogger(ClientProto.class);

	public final static Parser PARSER = (id, bytes) -> {
		MessageHandel.CenterMsg centerMsg = MessageHandel.CenterMsg.get(id);
		if (centerMsg != null) {
			Class className = centerMsg.getClassName();
			try {
				return (Message) MessageHandel.getMessageObject(className, bytes);
			} catch (Exception e) {
				logger.error("parse message error messageId :{} className:{}", id, className.getSimpleName());
			}
		}
		return null;
	};


	private final static Map<Integer, Handler> handlers;

	static {
		handlers = new HashMap<>();
		handlers.put(MessageHandel.HEART_REQ, HeartHandler.getInstance());
		handlers.put(MessageHandel.REGISTER, RegisterEventHandler.getInstance());
		handlers.put(MessageHandel.CenterMsg.SERVER_REQ.getId(), ReqServerInfoHandler.getInstance());


	}

	public final static Handlers HANDLERS = handlers::get;


	public final static Transfer<CenterClient, TCPMessage> TRANSFER = (routerClient, tcpMessage) -> false;
}
