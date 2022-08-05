package gate.client;

import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;
import net.safe.Safe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GateClient extends ClientHandler<GateClient, TCPMessage> {
	private final static Logger LOGGER = LoggerFactory.getLogger(GateClient.class);

	private long userId;
	private int gameId;
	private int roomId;
	private int hallId;

	public GateClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<GateClient>) client -> {
		});

		setSafe((Safe<GateClient, TCPMessage>) (id, msg) -> {
			return true;
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
