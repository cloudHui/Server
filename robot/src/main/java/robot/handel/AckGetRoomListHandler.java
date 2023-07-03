package robot.handel;

import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;

/**
 * 获取房间回复
 */
public class AckGetRoomListHandler implements Handler<HallProto.AckGetRoomList> {

	private final static Logger logger = LoggerFactory.getLogger(AckGetRoomListHandler.class);


	private static AckGetRoomListHandler instance = new AckGetRoomListHandler();

	public static AckGetRoomListHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, HallProto.AckGetRoomList ack, int mapId) {
		logger.error("get room ack:{}", ack.toString());
		return true;
	}
}
