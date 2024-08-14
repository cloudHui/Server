package robot.handel;

import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.RoomProto;

/**
 * 获取房间回复
 */
public class AckGetRoomListHandler implements Handler<RoomProto.AckGetRoomList> {

	private final static Logger logger = LoggerFactory.getLogger(AckGetRoomListHandler.class);


	private static AckGetRoomListHandler instance = new AckGetRoomListHandler();

	public static AckGetRoomListHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, long aLong, RoomProto.AckGetRoomList ack, int mapId) {
		logger.error("get room ack:{}", ack.toString());
		return true;
	}
}
