package gate.client;

import gate.Gate;
import msg.MessageId;
import msg.ServerType;
import net.client.handler.ClientHandler;
import net.connect.TCPConnect;
import net.message.TCPMaker;
import proto.ModelProto;
import utils.ServerManager;


public class GateTcpClient extends ClientHandler {

	private int roleId = 0;
	private int gameId = 0;
	private int hallId = 0;
	private int roomId = 0;

	public GateTcpClient() {
		super(ClientProto.PARSER, null, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent(client -> ClientProto.notServerBreak(roleId, gameId, hallId, roomId));

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
}