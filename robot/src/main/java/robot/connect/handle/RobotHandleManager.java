package robot.connect.handle;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import msg.registor.HandleTypeRegister;
import net.connect.handle.ConnectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import robot.connect.ConnectProcessor;

/**
 * @author admin
 * @className RobotHandleManager
 * @description
 * @createDate 2025/10/9 14:54
 */
public class RobotHandleManager {
	private final static Logger LOGGER = LoggerFactory.getLogger(RobotHandleManager.class);
	private static final Map<Class<?>, RobotHandle> handleMap = new HashMap<>();

	public static void init() {
		HandleTypeRegister.initClassFactory(RobotHandleManager.class, handleMap);
	}

	/**
	 * 发送消息
	 */
	public static void sendMsg(ConnectHandler serverClient, Message req, int msgId) {
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
				Message parser = ConnectProcessor.PARSER.parser(msgId, msg.getMessage());
				RobotHandle robotHandle = handleMap.get(parser.getClass());
				if (robotHandle == null) {
					LOGGER.error("sendMsg error un find handle:{}", parser.getClass().getSimpleName());
					return;
				}
				robotHandle.handle(parser, serverClient);
				LOGGER.info("handle:{} msg:{} cost:{}ms", parser.getClass().getSimpleName(),
						Integer.toHexString(msg.getMessageId()), (System.currentTimeMillis() - start));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
