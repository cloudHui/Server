package hall.client;

import hall.Hall;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;


public class HallClient extends ClientHandler<HallClient, TCPMessage> {

	public HallClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<HallClient>) client -> {
			Hall.getInstance().setGateClient(null);
		});
	}
}
