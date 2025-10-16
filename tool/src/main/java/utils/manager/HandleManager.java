package utils.manager;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import msg.registor.HandleTypeRegister;
import net.connect.handle.ConnectHandler;
import net.message.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author admin
 * @className HandleManager
 * @description
 * @createDate 2025/10/9 14:54
 */
public class HandleManager {
	private final static Logger LOGGER = LoggerFactory.getLogger(HandleManager.class);
	private static final Map<Class<?>, ConnectHandle> handleMap = new HashMap<>();

	public static void init(Class<?> aClass) {
		HandleTypeRegister.initClassFactory(aClass, handleMap);
	}

	/**
	 * 发送消息
	 */
	public static void sendMsg(int msgId, Message req, ConnectHandler serverClient, Parser parser) {
		sendMsg(msgId, req, serverClient, parser, 0);
	}

	/**
	 * 统一发送proto消息需要自行处理回调的消息
	 *
	 * @param msgId        消息id
	 * @param req          消息体
	 * @param serverClient 转过过来的链接
	 * @param parser       回调消息转化器
	 * @param sequence     序列号回复消息
	 */
	public static void sendMsg(int msgId, Message req, ConnectHandler serverClient, Parser parser, int sequence) {
		LOGGER.info("sendMsg:{}", req.getClass().getSimpleName());
		long start = System.currentTimeMillis();
		serverClient.sendMessageBackTcp(req, msgId, 3).whenComplete((msg, throwable) -> {
			try {
				if (throwable != null) {
					LOGGER.info("msg:{} back throwable:{}", Integer.toHexString(msg.getMessageId()), throwable.getMessage());
					return;
				}
				if (msg.getResult() != 0) {
					LOGGER.info("msg:{} result:{} error", Integer.toHexString(msg.getMessageId()), msg.getResult());
					return;
				}
				Message message = parser.parser(msgId, msg.getMessage());
				ConnectHandle connectHandle = handleMap.get(parser.getClass());
				if (connectHandle == null) {
					LOGGER.error("sendMsg error un find handle:{}", parser.getClass().getSimpleName());
					return;
				}
				connectHandle.handle(message, serverClient, sequence);
				LOGGER.info("handle:{} msg:{} cost:{}ms", parser.getClass().getSimpleName(),
						Integer.toHexString(msg.getMessageId()), (System.currentTimeMillis() - start));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
