package robot.connect.handle.lobby;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import net.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.LobbyProto;
import tools.manager.ConnectHandle;

@ProcessClass(LobbyProto.AckLogin.class)
public class AckLoginHandler implements ConnectHandle {
	private static final Logger logger = LoggerFactory.getLogger(AckLoginHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		if (message instanceof LobbyProto.AckLogin) {
			LobbyProto.AckLogin ack = (LobbyProto.AckLogin) message;
			logger.info("AckLogin: code={}, userId={}, nick={}",
					ack.getCode(), ack.getUserId(), ack.getNickName().toStringUtf8());
		}
	}
}
