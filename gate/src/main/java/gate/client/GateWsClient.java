package gate.client;

import msg.registor.message.HMsg;
import net.client.handler.WsClientHandler;
import net.message.TCPMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket客户端处理器
 * 管理WebSocket连接的客户端状态和行为
 */
public class GateWsClient extends WsClientHandler {
	private static final Logger logger = LoggerFactory.getLogger(GateWsClient.class);
	private int roleId; // 统一使用roleId命名
	private int gameId;
	private int hallId;
	private int roomId;

	public GateWsClient() {
		super(null, null, ClientProto.TRANSFER, TCPMaker.INSTANCE);

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