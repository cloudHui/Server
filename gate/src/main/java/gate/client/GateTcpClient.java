package gate.client;

import gate.Gate;
import msg.MessageId;
import msg.ServerType;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.connect.TCPConnect;
import net.message.TCPMaker;
import net.message.TCPMessage;
import net.safe.Safe;
import proto.ModelProto;
import utils.ServerManager;


public class GateTcpClient extends ClientHandler {

	private int roleId = 0;
	private int gameId = 0;
	private int hallId = 0;
	private int roomId = 0;

	public GateTcpClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent(client -> notServerBreak());

		setSafe((msgId) -> msgId == MessageId.HallMsg.REQ_LOGIN.getId() || roleId != 0);
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

	/**
	 * 通知服务器玩家离线
	 */
	public void notServerBreak() {
		ModelProto.NotBreak.Builder not = ModelProto.NotBreak.newBuilder();
		not.setUserId(getRoleId());
		ServerManager serverManager = Gate.getInstance().getServerManager();
		if (serverManager == null) {
			return;
		}
		TCPConnect serverClient = serverManager.getServerClient(ServerType.Game, getGameId());
		if (serverClient != null) {
			serverClient.sendMessage(MessageId.NOT_BREAK, not.build(), null);
		}
		serverClient = serverManager.getServerClient(ServerType.Hall, getHallId());
		if (serverClient != null) {
			serverClient.sendMessage(MessageId.NOT_BREAK, not.build(), null);
		}
		serverClient = serverManager.getServerClient(ServerType.Room, getHallId());
		if (serverClient != null) {
			serverClient.sendMessage(MessageId.NOT_BREAK, not.build(), null);
		}
	}
}
