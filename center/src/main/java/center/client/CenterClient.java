package center.client;

import msg.MessageHandel;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;
import net.safe.Safe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;


public class CenterClient extends ClientHandler<CenterClient, TCPMessage> {

	private static final Logger logger = LoggerFactory.getLogger(ClientProto.class);

	private ModelProto.ServerInfo serverInfo;

	public CenterClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<CenterClient>) client -> {
		});

		setSafe((Safe<CenterClient, TCPMessage>) (client, msg) -> msg.getMessageId() == MessageHandel.REQ_REGISTER || msg.getMessageId() == MessageHandel.HEART);
	}

	public ModelProto.ServerInfo getServerInfo() {
		return serverInfo;
	}

	public void setServerInfo(ModelProto.ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}
}
