package game.client;

import game.Game;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;


public class GameClient extends ClientHandler<GameClient, TCPMessage> {

	public GameClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<GameClient>) client -> {
			Game.getInstance().setGateClient(null);
		});
	}
}
