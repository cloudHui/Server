package hall.client;

import hall.Hall;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HallClient extends ClientHandler<HallClient, TCPMessage> {
	private final static Logger logger = LoggerFactory.getLogger(HallClient.class);

	public HallClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<HallClient>) client -> {
			Hall.getInstance().setGateClient(null);
		});
	}
}
