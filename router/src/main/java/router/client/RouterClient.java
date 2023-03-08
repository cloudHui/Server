package router.client;

import msg.MsgId;
import net.client.event.CloseEvent;
import net.client.handler.ClientHandler;
import net.message.TCPMaker;
import net.message.TCPMessage;
import net.safe.Safe;


public class RouterClient extends ClientHandler<RouterClient, TCPMessage> {

	private String ipConfig;

	public RouterClient() {
		super(ClientProto.PARSER, ClientProto.HANDLERS, ClientProto.TRANSFER, TCPMaker.INSTANCE);

		setCloseEvent((CloseEvent<RouterClient>) client -> {

		});

		setSafe((Safe<RouterClient, TCPMessage>) (client, msg) -> msg.getMessageId() == MsgId.REGISTER || msg.getMessageId() == MsgId.HEART_REQ);
	}

	public String getIpConfig() {
		return ipConfig;
	}

	public void setIpConfig(String ipConfig) {
		this.ipConfig = ipConfig;
	}
}
