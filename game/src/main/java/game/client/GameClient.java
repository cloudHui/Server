package game.client;

import game.Game;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;
import net.safe.Safe;


public class GameClient extends ClientHandler<GameClient, TCPMessage> {

	private long userId;
	private int hallId;

	public GameClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<GameClient>) client -> {
			Game.getInstance().setGateClient(null);
		});

		setSafe((Safe<GameClient, TCPMessage>) (gateClient, msg) -> true);
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public int getHallId() {
		return hallId;
	}

	public void setHallId(int hallId) {
		this.hallId = hallId;
	}
}
