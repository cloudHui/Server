package hall.client;

import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;
import net.safe.Safe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HallClient extends ClientHandler<HallClient, TCPMessage> {
	private final static Logger logger = LoggerFactory.getLogger(HallClient.class);

	private long userId;
	private int gameId;
	private int gateId;

	private boolean safe = false;

	public HallClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<HallClient>) client -> {
		});

		setSafe((Safe<HallClient, TCPMessage>) (gateClient, msg) -> {
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
}
