package gate.client;

import msg.registor.message.CMsg;
import msg.registor.message.HMsg;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP客户端处理器
 * 管理TCP连接的客户端状态和行为
 */
public class GateTcpClient extends ClientHandler {
	private static final Logger logger = LoggerFactory.getLogger(GateTcpClient.class);
	private int roleId = -1;
	private long mapId = -1;//当前进入的桌子号
	private int clubId;
	private int channel;

	public GateTcpClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		// 设置连接关闭事件处理
		setCloseEvent(client -> {
			logger.info("TCP客户端连接关闭, roleId: {}, clientId: {}", roleId, getId());
			ClientProto.notifyServerDisconnect(roleId, client);
		});

		// 设置消息安全验证
		setSafe((msgId) ->
				msgId == HMsg.REQ_LOGIN_MSG ||
						msgId == CMsg.REQ_REGISTER ||
						msgId == CMsg.HEART ||
						roleId != 0
		);

		logger.debug("创建新的TCP客户端处理器");
	}

	public int getRoleId() {
		return roleId;
	}

	public void setRoleId(int roleId) {
		this.roleId = roleId;
		logger.debug("设置用户角色ID: {}", roleId);
	}

	public int getClubId() {
		return clubId;
	}

	public void setClubId(int clubId) {
		this.clubId = clubId;
		logger.debug("设置俱乐部ID: {}", clubId);
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
		logger.debug("设置渠道ID: {}", channel);
	}

	public long getMapId() {
		return mapId;
	}

	public void setMapId(long mapId) {
		this.mapId = mapId;
	}
}