package robot.connect.handle.room;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import msg.registor.message.RMsg;
import net.connect.handle.ConnectHandler;
import proto.RoomProto;
import robot.Robot;
import robot.connect.handle.RobotHandle;

/**
 * 获取房间回复
 */
@ProcessClass(RoomProto.AckGetRoomList.class)
public class AckGetRoomListHandler implements RobotHandle {

	@Override
	public void handle(Message message, ConnectHandler serverClient) {
		if (message instanceof RoomProto.AckGetRoomList) {
			RoomProto.AckGetRoomList rooms = (RoomProto.AckGetRoomList) message;
			LOGGER.error("AckGetRoomList:{}", rooms.toString());
			if (rooms.getRoomListCount() > 0) {
				RoomProto.Room room = rooms.getRoomList(0);
				RoomProto.ReqJoinRoomTable.Builder createTable = RoomProto.ReqJoinRoomTable.newBuilder();
				createTable.setRoomId(room.getRoomId());
				Robot.getInstance().getClientSendMessage(RMsg.REQ_JOIN_ROOM_TABLE_MSG, createTable.build(), serverClient);
			}
		}
	}
}
