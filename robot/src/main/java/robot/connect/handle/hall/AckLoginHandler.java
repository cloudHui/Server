package robot.connect.handle.hall;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import msg.registor.message.RMsg;
import proto.HallProto;
import proto.RoomProto;
import robot.Robot;
import robot.connect.handle.RobotHandle;

/**
 * 登录回复
 */
@ProcessClass(HallProto.AckLogin.class)
public class AckLoginHandler implements RobotHandle {

	@Override
	public void handle(Message message) {
		if (message instanceof HallProto.AckLogin) {
			HallProto.AckLogin ack = (HallProto.AckLogin) message;
			LOGGER.error("AckLogin:{}", ack.toString());
			RoomProto.ReqGetRoomList.Builder builder = RoomProto.ReqGetRoomList.newBuilder();
			Robot.getInstance().getClientSendMessage(RMsg.REQ_ROOM_LIST_MSG, builder.build());
		}
	}
}
