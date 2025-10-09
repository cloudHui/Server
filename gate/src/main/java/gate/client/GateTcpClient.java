package gate.client;

import msg.registor.message.CMsg;
import msg.registor.message.HMsg;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;


public class GateTcpClient extends ClientHandler {

	private int roleId;
	private int gameId;
	private int hallId;
	private int roomId;

	private int clubId;
	private int channel;

	public GateTcpClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent(client -> ClientProto.notServerBreak(roleId, gameId, hallId, roomId, client));

		setSafe((msgId) -> msgId == HMsg.REQ_LOGIN_MSG || msgId == CMsg.REQ_REGISTER ||msgId == CMsg.HEART || roleId != 0);
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

	public int getClubId() {
		return clubId;
	}

	public void setClubId(int clubId) {
		this.clubId = clubId;
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}
}