package gate.client;

import msg.MessageId;
import net.client.handler.WsClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;


public class GateWsClient extends WsClientHandler {

	private int userId = 0;
	private int gameId = 0;
	private int hallId = 0;
	private int roomId = 0;

	public GateWsClient() {
		super(null, null, ClientProto.TRANSFER, TCPMaker.INSTANCE);
		TCPMessage tcpMessage = new TCPMessage(1, new byte[5], 0);
		sendMessage(tcpMessage);

		setCloseEvent(client -> ClientProto.notServerBreak(userId, gameId, hallId, roomId));

		setSafe((msgId) -> msgId == MessageId.HallMsg.REQ_LOGIN.getId() || userId != 0);
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
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
