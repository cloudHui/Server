package gate.client;

import msg.MessageHandel;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;
import net.safe.Safe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GateClient extends ClientHandler<GateClient, TCPMessage> {
	private final static Logger logger = LoggerFactory.getLogger(GateClient.class);

	private long userId;
	private int gameId;
	private int roomId;
	private int hallId;

	private boolean safe = false;

	public GateClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<GateClient>) client -> {
			//Todo
			//发送这个玩家下线到其他服务
			//删除这个玩家的链接和数据
			//记录等出日志
		});

		setSafe((Safe<GateClient, TCPMessage>) (gateClient, msg) -> {
			if (MessageHandel.GateMsg.LOGIN_REQ.getId() == msg.getMessageId()) {
				return true;
			}
			return safe;
		});
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public int getGameId() {
		return gameId;
	}

	public void setGameId(int gameId) {
		this.gameId = gameId;
	}

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	public int getHallId() {
		return hallId;
	}

	public void setHallId(int hallId) {
		this.hallId = hallId;
	}
}
