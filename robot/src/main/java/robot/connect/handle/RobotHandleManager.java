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
	 * 处理
	 */
	public static void handle(Message message) {
		RobotHandle robotHandle = handleMap.get(message.getClass());
		if (robotHandle == null) {
			LOGGER.error("handle error un find handle:{}", message.getClass().getSimpleName());
			return;
		}
		robotHandle.handle(message);
	}

	/**
	 * 发送消息
	 */
	public static void sendMsg(ConnectHandler serverClient, Message req, int msgId) {
		serverClient.sendMessage(req, msgId, 3).whenComplete((msg, e) -> handle(msg));
	}
}
