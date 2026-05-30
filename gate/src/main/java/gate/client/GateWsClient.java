package gate.client;

import gate.rate.ConnectionRateLimiter;
import io.netty.channel.ChannelHandler;
import msg.registor.message.HMsg;
import net.client.handler.WsClientHandler;
import net.message.TCPMessage;
import net.message.TCPMaker;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;

/**
 * WebSocket客户端处理器
 * 管理WebSocket连接的客户端状态和行为
 * 包含设备ID连接频率限制
 */
public class GateWsClient extends WsClientHandler {
	private static final Logger logger = LoggerFactory.getLogger(GateWsClient.class);

	/** 全局连接限频器 */
	private static final ConnectionRateLimiter rateLimiter = new ConnectionRateLimiter();

	private int roleId;
	private int gameId;
	private int hallId;
	private int roomId;

	public GateWsClient() {
		super(null, null, createRateLimitedTransfer(), TCPMaker.INSTANCE);

		// 设置连接关闭事件处理
		setCloseEvent(client -> {
			logger.info("WebSocket客户端连接关闭, roleId: {}, clientId: {}", roleId, getId());
			ClientProto.notifyServerDisconnect(roleId, client);
		});

		// 设置消息安全验证
		setSafe((msgId) ->
				msgId == HMsg.REQ_LOGIN_MSG ||
						roleId != 0
		);

		logger.debug("创建新的WebSocket客户端处理器");
	}

	/**
	 * 创建带限频的Transfer
	 * 在消息转发前检查登录消息的设备ID连接频率
	 */
	private static Transfer createRateLimitedTransfer() {
		return (ChannelHandler connectHandler, TCPMessage tcpMessage) -> {
			if (tcpMessage.getMessageId() == HMsg.REQ_LOGIN_MSG) {
				try {
					byte[] data = tcpMessage.getMessage();
					if (data != null && data.length > 0) {
						HallProto.ReqLogin login = HallProto.ReqLogin.parseFrom(data);
						String deviceId = login.getCert().toStringUtf8();
						if (!deviceId.isEmpty() && !rateLimiter.allow(deviceId)) {
							// 静默拒绝：关闭连接，不转发
							logger.warn("设备连接频率超限，静默拒绝, deviceId: {}", deviceId);
							if (connectHandler instanceof WsClientHandler) {
								((WsClientHandler) connectHandler).closeChannel();
							}
							return true;
						}
					}
				} catch (Exception e) {
					logger.error("限频检查异常", e);
				}
			}
			// 非登录消息或未超限 -> 交给原始Transfer处理
			return ClientProto.TRANSFER.isTransfer(connectHandler, tcpMessage);
		};
	}

	public int getRoleId() {
		return roleId;
	}

	public void setRoleId(int roleId) {
		this.roleId = roleId;
		logger.debug("设置WebSocket用户角色ID: {}", roleId);
	}

	public int getGameId() {
		return gameId;
	}

	public void setGameId(int gameId) {
		this.gameId = gameId;
	}

	public int getHallId() {
		return hallId;
	}

	public void setHallId(int hallId) {
		this.hallId = hallId;
	}

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}
}
