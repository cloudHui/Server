package robot.connect.handel;

import com.google.protobuf.Message;
import msg.HallMessageId;
import msg.RoomMessageId;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import proto.RoomProto;

/**
 * 登录回复
 */
@ProcessType(HallMessageId.ACK_LOGIN_MSG)
public class AckLoginHandler implements Handler {

	private final static Logger logger = LoggerFactory.getLogger(AckLoginHandler.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		HallProto.AckLogin ack = (HallProto.AckLogin) msg;
		logger.error("[login ack:{}]", ack.toString());
		RoomProto.ReqGetRoomList.Builder getRoom = RoomProto.ReqGetRoomList.newBuilder();
		sender.sendMessage(RoomMessageId.REQ_ROOM_LIST_MSG, getRoom.build());
		return true;
	}
}
