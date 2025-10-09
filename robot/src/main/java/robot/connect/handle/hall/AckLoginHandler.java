package robot.connect.handle.hall;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.HMsg;
import msg.registor.message.RMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import proto.RoomProto;
import robot.Robot;

/**
 * 登录回复
 */
@ProcessType(value = HMsg.ACK_LOGIN_MSG, trans = HallProto.AckLogin.class)
public class AckLoginHandler implements Handler {

	private final static Logger logger = LoggerFactory.getLogger(AckLoginHandler.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		HallProto.AckLogin ack = (HallProto.AckLogin) msg;
		logger.error("[login ack:{}]", ack.toString());
		RoomProto.ReqGetRoomList.Builder builder = RoomProto.ReqGetRoomList.newBuilder();

		Robot.getInstance().getClientSendMessage(ServerType.Room, RMsg.REQ_ROOM_LIST_MSG, builder.build());
		return true;
	}
}
