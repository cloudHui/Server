package robot.handel;

import com.google.protobuf.Message;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 获取房间回复
 */
public class AckGetRoomListHandler implements Handler {

	private final static Logger logger = LoggerFactory.getLogger(AckGetRoomListHandler.class);


	private static final AckGetRoomListHandler instance = new AckGetRoomListHandler();

	public static AckGetRoomListHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, int clientId, Message ack, int mapId , long sequence) {
		logger.error("get room ack:{}", ack.toString());
		return true;
	}
}
