package gate.client;

import gate.rate.ConnectionRateLimiter;
import io.netty.channel.ChannelHandler;
import msg.registor.message.CMsg;
import msg.registor.message.HMsg;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;

/**
 * TCP客户端处理器
 * 管理TCP连接的客户端状态和行为
 * 包含设备ID连接频率限制
 */
public class GateTcpClient extends ClientHandler {
	private static final Logger logger = LoggerFactory.getLogger(GateTcpClient.class);

	/** 全局连接限频器(与WsClient共享) */
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
				msgId == HMsg.REQ_LOGIN_MSG ||
						msgId == CMsg.REQ_REGISTER ||
						msgId == CMsg.HEART ||
						roleId != 0
		);

		logger.debug("创建新的TCP客户端处理器");
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
							logger.warn("设备连接频率超限，静默拒绝, deviceId: {}", deviceId);
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