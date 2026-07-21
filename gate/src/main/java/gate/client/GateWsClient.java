package gate.client;

import gate.rate.ConnectionRateLimiter;
import io.netty.channel.ChannelHandler;
import msg.registor.message.LMsg;
import net.client.handler.WsClientHandler;
import net.message.TCPMessage;
import net.message.TCPMaker;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.LobbyProto;

/**
 * WebSocket客户端处理器
 */
public class GateWsClient extends WsClientHandler {
	private static final Logger logger = LoggerFactory.getLogger(GateWsClient.class);

	private static final ConnectionRateLimiter rateLimiter = new ConnectionRateLimiter();

	private int roleId;
	private int gameId;
	private int lobbyId;
	private int roomId;

	public GateWsClient() {
		super(null, null, createRateLimitedTransfer(), TCPMaker.INSTANCE);

		setCloseEvent(client -> {
			logger.info("WebSocket客户端连接关闭, roleId: {}, clientId: {}", roleId, getId());
			ClientProto.notifyServerDisconnect(roleId, client);
		});

		setSafe((msgId) ->
				msgId == LMsg.REQ_LOGIN_MSG ||
						msgId == LMsg.REQ_REGISTER_MSG ||
						roleId != 0
		);

		logger.debug("创建新的WebSocket客户端处理器");
	}

	private static Transfer createRateLimitedTransfer() {
		return (ChannelHandler connectHandler, TCPMessage tcpMessage) -> {
			if (tcpMessage.getMessageId() == LMsg.REQ_LOGIN_MSG
					|| tcpMessage.getMessageId() == LMsg.REQ_REGISTER_MSG) {
				try {
					byte[] data = tcpMessage.getMessage();
					if (data != null && data.length > 0) {
						String key = "";
						if (tcpMessage.getMessageId() == LMsg.REQ_LOGIN_MSG) {
							LobbyProto.ReqLogin login = LobbyProto.ReqLogin.parseFrom(data);
							key = login.getUsername().toStringUtf8();
							if (key.isEmpty()) {
								key = login.getToken().toStringUtf8();
							}
						} else {
							LobbyProto.ReqUserRegister reg = LobbyProto.ReqUserRegister.parseFrom(data);
							key = reg.getUsername().toStringUtf8();
						}
						if (!key.isEmpty() && !rateLimiter.allow(key)) {
							logger.warn("连接频率超限，静默拒绝, key: {}", key);
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
			return ClientProto.TRANSFER.isTransfer(connectHandler, tcpMessage);
		};
	}

	public int getRoleId() {
		return roleId;
	}

	public void setRoleId(int roleId) {
		this.roleId = roleId;
	}

	public int getGameId() {
		return gameId;
	}

	public void setGameId(int gameId) {
		this.gameId = gameId;
	}

	public int getLobbyId() {
		return lobbyId;
	}

	public void setLobbyId(int lobbyId) {
		this.lobbyId = lobbyId;
	}

	/** @deprecated use getLobbyId */
	public int getHallId() {
		return lobbyId;
	}

	public void setHallId(int hallId) {
		this.lobbyId = hallId;
	}

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}
}
