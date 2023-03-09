package game.client;

import msg.MessageHandel;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;
import net.safe.Safe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GameClient extends ClientHandler<GameClient, TCPMessage> {
	private final static Logger logger = LoggerFactory.getLogger(GameClient.class);

	private long userId;
	private int hallId;

	private boolean safe = false;

	public GameClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<GameClient>) client -> {
		});

		setSafe((Safe<GameClient, TCPMessage>) (gateClient, msg) -> {
			return safe;
		});
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
