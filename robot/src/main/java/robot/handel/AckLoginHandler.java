package robot.handel;

import com.google.protobuf.Message;
import msg.MessageId;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import proto.RoomProto;

/**
 * 登录回复
 */
public class AckLoginHandler implements Handler {

	private final static Logger logger = LoggerFactory.getLogger(AckLoginHandler.class);


	private static final AckLoginHandler instance = new AckLoginHandler();

	public static AckLoginHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		HallProto.AckLogin ack = (HallProto.AckLogin) msg;
		logger.error("login ack:{}", ack.toString());
		RoomProto.ReqGetRoomList.Builder getRoom = RoomProto.ReqGetRoomList.newBuilder();
		sender.sendMessage(MessageId.RoomMsg.REQ_ROOM_LIST.getId(), getRoom.build(), null);
		return true;
	}
}
