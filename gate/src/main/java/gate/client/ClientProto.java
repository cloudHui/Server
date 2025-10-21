package gate.client;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.ByteString;
import gate.Gate;
import gate.client.handle.back.BackHandleManager;
import io.netty.channel.ChannelHandler;
import msg.registor.HandleTypeRegister;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import msg.registor.message.HMsg;
import net.client.handler.ClientHandler;
import net.client.handler.WsClientHandler;
import net.connect.handle.ConnectHandler;
import net.handler.Handler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ConstProto;
import proto.HallProto;
import proto.ModelProto;
import utils.ServerManager;

/**
 * 客户端协议处理器
 * 负责消息解析、转发和客户端管理
 */
public class ClientProto {
	private static final Logger logger = LoggerFactory.getLogger(ClientProto.class);

	// 消息转发接口
	public static final Transfer TRANSFER = ClientProto::transferMessage;
	public static final Parser PARSER = HandleTypeRegister::parseMessage;
	private static final Map<Integer, Handler> HANDLER_MAP = new HashMap<>();
	public static final Handlers HANDLERS = HANDLER_MAP::get;

	// 服务器请求超时时间（秒）
	private static final int SERVER_REQUEST_TIMEOUT = 3;

	/**
	 * 初始化协议处理器
	 */
	public static void init() {
		try {
			// 绑定专用服务器消息处理
			HandleTypeRegister.initFactory(ClientProto.class, HANDLER_MAP);
			// 绑定通用服务器消息处理
			HandleTypeRegister.initFactory(HANDLER_MAP);
			logger.info("ClientProto初始化完成,注册处理器数量: {}", HANDLER_MAP.size());
		} catch (Exception e) {
			logger.error("ClientProto初始化失败", e);
			throw new RuntimeException("ClientProto初始化失败", e);
		}
	}

	/**
	 * 消息转发到后端服务器
	 *
	 * @param channelHandler 客户端连接
	 * @param tcpMessage     TCP消息
	 * @return 是否成功处理
	 */
	private static boolean transferMessage(ChannelHandler channelHandler, TCPMessage tcpMessage) {
		GateTcpClient client = (GateTcpClient) channelHandler;
		int msgId = tcpMessage.getMessageId();

		int sequence = tcpMessage.getSequence();

		setClientMap(tcpMessage, client);
		ConnectHandler serverConnection = getTargetServerConnection(msgId);
		if (serverConnection == null) {
			logger.error("无法找到目标服务器连接, msgId: {}", Integer.toHexString(msgId));
			return false;
		}

		return sendMessageToServer(serverConnection, tcpMessage, sequence, client);
	}

	/**
	 * 设置链接id, 用户id,和桌子号
	 *
	 * @param tcpMessage 消息
	 * @param client     链接
	 */
	private static void setClientMap(TCPMessage tcpMessage, GateTcpClient client) {
		tcpMessage.setClientId(client.getRoleId() == -1 ? client.getId() : client.getRoleId());
		tcpMessage.setMapId(client.getMapId() == -1 ? client.getRoleId() : client.getMapId());//没登陆是玩家id 登录后是桌子id
	}

	/**
	 * 发送消息到后端服务器
	 */
	private static boolean sendMessageToServer(ConnectHandler serverConnection,
											   TCPMessage tcpMessage, int sequence,
											   GateTcpClient client) {
		long startTime = System.currentTimeMillis();
		int msgId = tcpMessage.getMessageId();

		serverConnection.sendTcpMessage(tcpMessage, SERVER_REQUEST_TIMEOUT)
				.whenComplete((response, error) -> {
					if (error != null) {
						handleSendError(error, msgId, serverConnection, client);
					} else {
						handleServerResponse(response, sequence, startTime, client, msgId);
					}
				});

		return true;
	}

	/**
	 * 处理发送错误
	 */
	private static void handleSendError(Throwable error, int msgId, ConnectHandler server, GateTcpClient client) {
		logger.error("发送消息到服务器失败, msgId: {}, server: {}, error: {}", Integer.toHexString(msgId), server.getConnectServer(), error.getMessage());
		client.sendMessage(TCPMessage.newInstance(ConstProto.Result.TIME_OUT_VALUE));
	}

	/**
	 * 处理服务器响应
	 */
	private static void handleServerResponse(TCPMessage response, int sequence, long startTime, GateTcpClient client, int msgId) {
		try {
			response.setSequence(sequence);
			forwardResponseToClient(response, startTime, client);
		} catch (Exception e) {
			logger.error("处理服务器响应失败, msgId: {}, clientId: {}, error: {}", Integer.toHexString(msgId), client.getId(), e.getMessage(), e);
		}
	}

	/**
	 * 转发响应到客户端
	 */
	private static void forwardResponseToClient(TCPMessage response, long startTime, GateTcpClient client) {
		int msgId = response.getMessageId();

		BackHandleManager.handle(response, client);
		client.sendMessage(response);

		long costTime = System.currentTimeMillis() - startTime;
		logger.info("消息转发成功, msgId: {}, userId: {}, 耗时: {}ms", Integer.toHexString(msgId), client.getRoleId(), costTime);
	}

	/**
	 * 处理客户端特殊响应（如登录响应）
	 */
	private static void processClientResponse(TCPMessage response, GateTcpClient client) {
		if (response.getMessageId() == HMsg.ACK_LOGIN_MSG) {
			//Todo 有其他消息字段 gate要存储
			processLoginResponse(response, client);
		}
	}


	/**
	 * 获取目标服务器连接
	 */
	private static ConnectHandler getTargetServerConnection(int msgId) {
		ServerType serverType = getServerTypeByMessageId(msgId);
		if (serverType == null) {
			logger.error("无法根据消息ID确定服务器类型, msgId: {}", Integer.toHexString(msgId));
			return null;
		}

		ConnectHandler connection = Gate.getInstance().getServerManager().getServerClient(serverType);
		if (connection == null) {
			logger.warn("服务器连接不可用, serverType: {}, msgId: {}", serverType, Integer.toHexString(msgId));
		}

		return connection;
	}

	/**
	 * 根据消息ID获取服务器类型
	 */
	private static ServerType getServerTypeByMessageId(int msgId) {
		if ((msgId & CMsg.GAME_TYPE) != 0) {
			return ServerType.Game;
		} else if ((msgId & CMsg.HALL_TYPE) != 0) {
			return ServerType.Hall;
		} else if ((msgId & CMsg.ROOM_TYPE) != 0) {
			return ServerType.Room;
		}

		logger.debug("未知消息类型, msgId: {}", Integer.toHexString(msgId));
		return null;
	}

	/**
	 * 通知服务器玩家断开连接
	 */
	protected static void notifyServerDisconnect(int userId, ChannelHandler handler) {
		logger.info("通知服务器玩家断开连接, userId: {}", userId);

		ModelProto.NotBreak.Builder disconnectNotify = ModelProto.NotBreak.newBuilder();
		disconnectNotify.setUserId(userId);

		ServerManager serverManager = Gate.getInstance().getServerManager();
		if (serverManager == null) {
			logger.error("服务器管理器未初始化");
			return;
		}

		// 通知所有相关服务器
		notifyAllServersDisconnect(disconnectNotify.build(), handler);
	}

	/**
	 * 通知所有服务器玩家断开
	 */
	private static void notifyAllServersDisconnect(ModelProto.NotBreak disconnectNotify, ChannelHandler handler) {
		sendDisconnectNotify(ServerType.Game, disconnectNotify);
		sendDisconnectNotify(ServerType.Hall, disconnectNotify);
		sendDisconnectNotify(ServerType.Room, disconnectNotify);

		// 通知中心服务器
		notifyCenterServerDisconnect(disconnectNotify, handler);
	}

	/**
	 * 发送断开连接通知到指定服务器
	 */
	private static void sendDisconnectNotify(ServerType serverType, ModelProto.NotBreak disconnectNotify) {
		ConnectHandler serverConnection = Gate.getInstance().getServerManager().getServerClient(serverType);
		if (serverConnection != null) {
			serverConnection.sendMessage(CMsg.NOT_BREAK, disconnectNotify);
			logger.debug("已通知服务器玩家断开, serverType: {}", serverType);
		} else {
			logger.debug("服务器连接不存在, serverType: {}", serverType);
		}
	}

	/**
	 * 通知中心服务器断开连接
	 */
	private static void notifyCenterServerDisconnect(ModelProto.NotBreak disconnectNotify, ChannelHandler handler) {
		ConnectHandler centerConnection = Gate.getInstance().getServerManager().getServerClient(ServerType.Center);
		if (centerConnection != null) {
			ModelProto.NotBreak.Builder notifyBuilder = disconnectNotify.toBuilder();
			setDisconnectCertificate(notifyBuilder, handler);
			centerConnection.sendMessage(CMsg.NOT_BREAK, notifyBuilder.build());
			logger.debug("已通知中心服务器玩家断开");
		}
	}

	/**
	 * 设置断开连接的证书信息
	 */
	private static void setDisconnectCertificate(ModelProto.NotBreak.Builder notifyBuilder, ChannelHandler handler) {
		String hostAddress = getHandlerHostAddress(handler);
		if (hostAddress != null) {
			notifyBuilder.setCert(ByteString.copyFromUtf8(hostAddress));
		}
	}

	/**
	 * 获取处理器的远程主机地址
	 */
	private static String getHandlerHostAddress(ChannelHandler handler) {
		if (handler instanceof ClientHandler) {
			return ClientHandler.getRemoteIP((ClientHandler) handler).getHostName();
		} else if (handler instanceof WsClientHandler) {
			return WsClientHandler.getRemoteIP((WsClientHandler) handler).getHostName();
		}
		return null;
	}
}