package utils.manager;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import msg.registor.HandleTypeRegister;
import net.connect.handle.ConnectHandler;
import net.message.Parser;
import net.message.TCPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author admin
 * @className HandleManager
 * @description 消息处理器管理器，负责管理各种消息处理器并处理消息的发送和回调
 * @createDate 2025/10/9 14:54
 */
public class HandleManager {
	private final static Logger LOGGER = LoggerFactory.getLogger(HandleManager.class);
	private static final Map<Class<?>, ConnectHandle> handleMap = new HashMap<>();

	private static final int SEND_MESSAGE_TIME_OUT = 3;//发送消息返回超时时间，单位：秒

	/**
	 * 初始化处理器映射
	 *
	 * @param aClass 注册类，用于初始化处理器工厂
	 */
	public static void init(Class<?> aClass) {
		try {
			HandleTypeRegister.initClassFactory(aClass, handleMap);
		} catch (Exception e) {
			LOGGER.error("HandleManager初始化失败，类: {}，错误: {}", aClass.getSimpleName(), e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * 发送消息（简化版本）
	 *
	 * @param msgId        消息ID，用于标识消息类型
	 * @param req          消息体，Protobuf消息对象
	 * @param serverClient 服务器客户端连接处理器
	 * @param parser       消息解析器，用于解析返回的消息
	 */
	public static void sendMsg(int msgId, Message req, ConnectHandler serverClient, Parser parser) {
		sendMsg(msgId, req, serverClient, parser, 0, 0, false);
	}

	/**
	 * 发送消息（带序列号和错误回传标志）
	 *
	 * @param msgId        消息ID，用于标识消息类型
	 * @param req          消息体，Protobuf消息对象
	 * @param serverClient 服务器客户端连接处理器
	 * @param parser       消息解析器，用于解析返回的消息
	 * @param sequence     序列号，用于消息的排序和匹配
	 * @param backError    是否在发生错误时回传错误消息给客户端
	 */
	public static void sendMsg(int msgId, Message req, ConnectHandler serverClient, Parser parser, int sequence, boolean backError) {
		sendMsg(msgId, req, serverClient, parser, sequence, 0, backError);
	}

	/**
	 * 统一发送proto消息，需要自行处理回调的消息
	 *
	 * @param msgId     消息ID，用于标识消息类型
	 * @param req       消息体，Protobuf消息对象
	 * @param handler   连接处理器，负责实际的网络通信
	 * @param parser    消息解析器，用于将返回的字节数据解析为Protobuf消息
	 * @param sequence  序列号，用于请求和响应的匹配，确保消息顺序
	 * @param userId    用户ID，标识消息所属的用户
	 * @param backError 是否在发生错误时回传错误消息给客户端
	 */
	public static void sendMsg(int msgId, Message req, ConnectHandler handler, Parser parser, int sequence, int userId, boolean backError) {
		LOGGER.info("发送消息 - 消息类型:{}，用户ID:{}，消息ID:0x{}，序列号:{}", req.getClass().getSimpleName(), userId, Integer.toHexString(msgId), sequence);

		long start = System.currentTimeMillis();

		// 发送消息并设置超时回调
		handler.sendMessageBackTcp(req, msgId, SEND_MESSAGE_TIME_OUT).whenComplete((msg, throwable) -> {
			try {
				if (throwable != null) {
					LOGGER.error("消息发送异常 - 消息ID:0x{}，用户ID:{}，异常信息:{}", Integer.toHexString(msgId), userId,
							throwable.getMessage(), throwable);
					return;
				}

				if (msg.getResult() != 0) {
					LOGGER.warn("消息处理失败 - 消息ID:0x{}，返回码:{}，用户ID:{}", Integer.toHexString(msg.getMessageId()),
							msg.getResult(), userId);
					handleErrorResponse(userId, msg.getResult(), handler, backError);
					return;
				}

				// 解析返回消息
				Message message = parser.parser(msg.getMessageId(), msg.getMessage());
				ConnectHandle connectHandle = handleMap.get(message.getClass());
				if (connectHandle == null) {
					LOGGER.error("未找到对应的消息处理器 - 解析器类型:{}，消息ID:0x{}，用户ID:{}",
							parser.getClass().getSimpleName(), Integer.toHexString(msg.getMessageId()), userId);
					return;
				}

				// 调用对应的处理器处理消息
				connectHandle.handle(message, handler, sequence, userId);
				LOGGER.info("消息处理完成 - 处理器:{}，消息ID:0x{}，用户ID:{}，耗时:{}ms",
						parser.getClass().getSimpleName(), Integer.toHexString(msg.getMessageId()),
						userId, (System.currentTimeMillis() - start));

			} catch (Exception e) {
				LOGGER.error("消息回调处理异常 - 消息ID:0x{}，用户ID:{}，异常信息:{}", Integer.toHexString(msgId), userId, e.getMessage(), e);
			}
		});
	}

	/**
	 * 处理错误响应
	 *
	 * @param userId    用户ID，标识发生错误的用户
	 * @param errorCode 错误码，表示具体的错误类型
	 * @param handler   连接处理器，用于可能的错误消息回传
	 * @param backError 是否将错误码回传给客户端
	 */
	private static void handleErrorResponse(int userId, int errorCode, ConnectHandler handler, boolean backError) {
		if (backError) {
			LOGGER.debug("回传错误码给客户端 - 用户ID:{}，错误码:{}", userId, errorCode);
			handler.sendMessage(TCPMessage.newInstance(errorCode));
		}
	}
}