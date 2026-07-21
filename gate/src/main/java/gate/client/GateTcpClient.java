package gate.client;

import gate.rate.ConnectionRateLimiter;
import io.netty.channel.ChannelHandler;
import msg.registor.message.CMsg;
import msg.registor.message.LMsg;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.LobbyProto;

/**
 * TCP客户端处理器
 */
public class GateTcpClient extends ClientHandler {
	private static final Logger logger = LoggerFactory.getLogger(GateTcpClient.class);

	private static final ConnectionRateLimiter rateLimiter = new ConnectionRateLimiter();

	private int roleId;
	private long mapId = -1;
	private int clubId;
	private int channel;

	public GateTcpClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, createRateLimitedTransfer(), TCPMaker.INSTANCE);

		setCloseEvent(client -> {
			logger.info("TCP客户端连接关闭, roleId: {}, clientId: {}", roleId, getId());
			ClientProto.notifyServerDisconnect(roleId, client);
		});

		setSafe((msgId) ->
				msgId == LMsg.REQ_LOGIN_MSG ||
						msgId == LMsg.REQ_REGISTER_MSG ||
						msgId == CMsg.REQ_REGISTER ||
						msgId == CMsg.HEART ||
						roleId != 0
		);

		logger.debug("创建新的TCP客户端处理器");
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
							if (connectHandler instanceof ClientHandler) {
								((ClientHandler) connectHandler).closeChannel();
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

	public int getRoleId() { return roleId; }
	public void setRoleId(int roleId) { this.roleId = roleId; }
	public int getClubId() { return clubId; }
	public void setClubId(int clubId) { this.clubId = clubId; }
	public int getChannel() { return channel; }
	public void setChannel(int channel) { this.channel = channel; }
	public long getMapId() { return mapId; }
	public void setMapId(long mapId) { this.mapId = mapId; }
}
