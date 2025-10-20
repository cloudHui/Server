package gate.connect;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import io.netty.channel.ChannelHandler;
import msg.annotation.ProcessMethod;
import msg.registor.HandleTypeRegister;
import msg.registor.message.GMsg;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;

public class ConnectProcessor {

	private final static Map<Integer, Handler> HANDLER_MAP = new HashMap<>();

	private final static Map<Integer, Method> METHOD_MAP = new HashMap<>();

	public final static Parser PARSER = HandleTypeRegister::parseMessage;

	public final static Handlers HANDLERS = HANDLER_MAP::get;

	public final static Transfer TRANSFER = ConnectProcessor::handleServerTrans;

	public static void init() {
		HandleTypeRegister.initFactory(ConnectProcessor.class, HANDLER_MAP);
		initMethodMap();
	}

	/**
	 * 初始化服务转发消息处理
	 */
	private static void initMethodMap() {
		Class<ConnectProcessor> aClass = ConnectProcessor.class;
		Method[] methods = aClass.getMethods();
		ProcessMethod annotation;
		for (Method method : methods) {
			annotation = method.getAnnotation(ProcessMethod.class);
			if (annotation == null) {
				continue;
			}
			METHOD_MAP.put(annotation.value(), method);
		}
	}

	/**
	 * 处理服务转发消息特殊处理
	 *
	 * @param connectHandler 服务链接
	 * @param tcpMessage     消息内容
	 * @return 是否转发
	 */
	private static boolean handleServerTrans(ChannelHandler connectHandler, TCPMessage tcpMessage) {
		Method method = METHOD_MAP.get(tcpMessage.getMessageId());

		if (method == null) {
			return false;
		}
		try {
			method.invoke(ConnectProcessor.class, connectHandler, tcpMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	@ProcessMethod(GMsg.NOT_CARD)
	private static void handleNotCard(ChannelHandler connectHandler, TCPMessage tcpMessage) {
	}

	@ProcessMethod(GMsg.NOT_OP)
	private static void handleNotOp(ChannelHandler connectHandler, TCPMessage tcpMessage) {
	}
}
