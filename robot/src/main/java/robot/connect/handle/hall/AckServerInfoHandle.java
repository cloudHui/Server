package robot.connect.handle.hall;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import msg.registor.enums.ServerType;
import msg.registor.message.RMsg;
import proto.HallProto;
import proto.RoomProto;
import robot.Robot;
import robot.connect.handle.RobotHandle;

/**
 * 登录信息回复
 */
@ProcessClass(HallProto.AckLogin.class)
public class AckServerInfoHandle implements RobotHandle {

	@Override
	public void handle(Message message) {
		if (message instanceof HallProto.AckLogin) {
			HallProto.AckLogin ack = (HallProto.AckLogin) message;
			LOGGER.error("AckLogin:{}", ack.getUserId() + " " + ack.getCert());
			Robot.getInstance().getClientSendMessage(ServerType.Room, RMsg.REQ_ROOM_LIST_MSG, RoomProto.ReqGetRoomList.newBuilder().build());
		}
	}
}