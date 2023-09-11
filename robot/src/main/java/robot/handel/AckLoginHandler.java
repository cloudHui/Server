package robot.handel;

import msg.Message;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import proto.RoomProto;

/**
 * 登录回复
 */
public class AckLoginHandler implements Handler<HallProto.AckLogin> {

	private final static Logger logger = LoggerFactory.getLogger(AckLoginHandler.class);


	private static AckLoginHandler instance = new AckLoginHandler();

	public static AckLoginHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, HallProto.AckLogin ack, int mapId) {
		logger.error("login ack:{}", ack.toString());
		RoomProto.ReqGetRoomList.Builder getRoom = RoomProto.ReqGetRoomList.newBuilder();
		sender.sendMessage(Message.RoomMsg.REQ_ROOM_LIST.getId(), getRoom.build(), null);
		return true;
	}
}
