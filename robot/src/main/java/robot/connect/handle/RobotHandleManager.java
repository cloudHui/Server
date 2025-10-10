package robot.connect.handle;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import msg.registor.HandleTypeRegister;
import net.connect.handle.ConnectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		HandleTypeRegister.initClassFactory(RobotHandleManager.class, handleMap, null, null);
	}

	/**
	 * 发送消息
	 */
	public static void sendMsg(ConnectHandler serverClient, Message req, int msgId) {
		LOGGER.info("sendMsg:{}", req.getClass().getSimpleName());
		long start = System.currentTimeMillis();
		serverClient.sendMessage(req, msgId, 3).whenComplete((msg, throwable) -> {
			try {
				LOGGER.info("msg:{} back result:{}",
						Integer.toHexString(HandleTypeRegister.parseMessageId(msg.getClass())),
						throwable == null ? "success" : throwable.getMessage());
				if (throwable == null) {
					RobotHandle robotHandle = handleMap.get(msg.getClass());
					if (robotHandle == null) {
						LOGGER.error("sendMsg error un find handle:{}", msg.getClass().getSimpleName());
						return;
					}

					robotHandle.handle(msg, serverClient);
					LOGGER.info("handle:{} msg:{} cost:{}ms", msg.getClass().getSimpleName(),
							Integer.toHexString(HandleTypeRegister.parseMessageId(msg.getClass())),
							(System.currentTimeMillis() - start));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
