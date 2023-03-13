package gate.client;

import msg.MessageHandel;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;
import net.safe.Safe;


public class GateClient extends ClientHandler<GateClient, TCPMessage> {

	private long userId = 0;
	private int gameId = 0;
	private int hallId = 0;

	public GateClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<GateClient>) client -> {
			//Todo
			//发送这个玩家下线到其他服务
			//删除这个玩家的链接和数据
			//记录等出日志
		});

		setSafe((Safe<GateClient, TCPMessage>) (gateClient, msg) -> msg.getMessageId() == MessageHandel.HallMsg.REQ_LOGIN.getId() || userId != 0);
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

	public int getHallId() {
		return hallId;
	}

	public void setHallId(int hallId) {
		this.hallId = hallId;
	}
}
