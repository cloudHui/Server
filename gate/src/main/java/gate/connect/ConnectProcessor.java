package gate.connect;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import gate.client.GateTcpClient;
import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import msg.annotation.ProcessMethod;
import msg.registor.HandleTypeRegister;
import msg.registor.message.GMsg;
import net.client.Sender;
import net.client.handler.ClientHandler;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;

public class ConnectProcessor {
	private static final Logger logger = LoggerFactory.getLogger(ConnectProcessor.class);

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
		Method[] methods = aClass.getDeclaredMethods();
		ProcessMethod annotation;
		for (Method method : methods) {
			annotation = method.getAnnotation(ProcessMethod.class);
			if (annotation == null) {
				continue;
			}
			method.setAccessible(true);
			METHOD_MAP.put(annotation.value(), method);
		}
		logger.info("ConnectProcessor 推送转发注册完成, count: {}", METHOD_MAP.size());
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
		// 带 sequence 的请求回复走 completer，不要当推送吞掉（入桌/出牌/离开）
		int msgId = tcpMessage.getMessageId();
		if (tcpMessage.getSequence() != 0
				&& (msgId == GMsg.ACK_ENTER_TABLE_MSG
				|| msgId == GMsg.ACK_OP
				|| msgId == GMsg.ACK_LEAVE)) {
			return false;
		}
		try {
			method.invoke(null, connectHandler, tcpMessage);
		} catch (Exception e) {
			logger.error("处理服务转发消息失败, msgId: {}", Integer.toHexString(tcpMessage.getMessageId()), e);
		}
		return true;
	}

	/** game→gate 推送：按 roleId(clientId) 转发到玩家连接 */
	private static void forwardToPlayer(TCPMessage tcpMessage) {
		int roleId = tcpMessage.getClientId();
		if (roleId == 0) {
			logger.warn("推送缺少 roleId, msgId: 0x{}", Integer.toHexString(tcpMessage.getMessageId()));
			return;
		}
		GateTcpClient target = findClientByRoleId(roleId);
		if (target == null) {
			logger.warn("找不到玩家连接, roleId: {}, msgId: 0x{}",
					roleId, Integer.toHexString(tcpMessage.getMessageId()));
			return;
		}
		target.sendMessage(tcpMessage);
		logger.debug("已转发推送, roleId: {}, msgId: 0x{}, mapId: {}",
				roleId, Integer.toHexString(tcpMessage.getMessageId()), tcpMessage.getMapId());
	}

	private static GateTcpClient findClientByRoleId(int roleId) {
		for (Map.Entry<Integer, Sender> entry : ClientHandler.getAllClient().entrySet()) {
			Sender sender = entry.getValue();
			if (sender instanceof GateTcpClient) {
				GateTcpClient client = (GateTcpClient) sender;
				if (client.getRoleId() == roleId) {
					return client;
				}
			}
		}
		return null;
	}

	@ProcessMethod(GMsg.NOT_CARD)
	private static void handleNotCard(ChannelHandler connectHandler, TCPMessage tcpMessage) {
		forwardToPlayer(tcpMessage);
	}

	@ProcessMethod(GMsg.NOT_OP)
	private static void handleNotOp(ChannelHandler connectHandler, TCPMessage tcpMessage) {
		forwardToPlayer(tcpMessage);
	}

	@ProcessMethod(GMsg.ACK_OP)
	private static void handleAckOp(ChannelHandler connectHandler, TCPMessage tcpMessage) {
		forwardToPlayer(tcpMessage);
	}

	@ProcessMethod(GMsg.NOT_RESULT)
	private static void handleNotResult(ChannelHandler connectHandler, TCPMessage tcpMessage) {
		forwardToPlayer(tcpMessage);
	}

	@ProcessMethod(GMsg.NOT_STATE)
	private static void handleNotState(ChannelHandler connectHandler, TCPMessage tcpMessage) {
		forwardToPlayer(tcpMessage);
	}

	@ProcessMethod(GMsg.NOT_TABLE_STATE)
	private static void handleNotTableState(ChannelHandler connectHandler, TCPMessage tcpMessage) {
		forwardToPlayer(tcpMessage);
	}

	@ProcessMethod(GMsg.MJ_TILE_NOT)
	private static void handleMjTileNot(ChannelHandler connectHandler, TCPMessage tcpMessage) {
		forwardToPlayer(tcpMessage);
	}

	@ProcessMethod(GMsg.NOT_ROUND_RESULT)
	private static void handleNotRoundResult(ChannelHandler connectHandler, TCPMessage tcpMessage) {
		forwardToPlayer(tcpMessage);
	}

	@ProcessMethod(GMsg.NOT_GAME_RESULT)
	private static void handleNotGameResult(ChannelHandler connectHandler, TCPMessage tcpMessage) {
		forwardToPlayer(tcpMessage);
	}

	@ProcessMethod(GMsg.ACK_ENTER_TABLE_MSG)
	private static void handleSeatUpdate(ChannelHandler connectHandler, TCPMessage tcpMessage) {
		// 补机器人后的座位刷新（无 sequence 的推送）
		if (tcpMessage.getSequence() == 0) {
			forwardToPlayer(tcpMessage);
		}
	}
}
