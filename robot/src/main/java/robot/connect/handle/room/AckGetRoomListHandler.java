package robot.connect.handle.room;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import msg.registor.message.RMsg;
import proto.RoomProto;
import robot.Robot;
import robot.connect.handle.RobotHandle;

/**
 * 获取房间回复
 */
@ProcessClass(RoomProto.AckGetRoomList.class)
public class AckGetRoomListHandler implements RobotHandle {

	@Override
	public void handle(Message message) {
		if (message instanceof RoomProto.AckGetRoomList) {
			RoomProto.AckGetRoomList rooms = (RoomProto.AckGetRoomList) message;
			LOGGER.error("AckGetRoomList:{}", rooms.toString());
			if (rooms.getRoomListCount() > 0) {
				RoomProto.Room room = rooms.getRoomList(0);
				RoomProto.ReqCreateRoomTable.Builder createTable = RoomProto.ReqCreateRoomTable.newBuilder();
				createTable.setConfigTypeId(room.getConfigTypeId());
				Robot.getInstance().getClientSendMessage(RMsg.REQ_CREATE_ROOM_TABLE_MSG, createTable.build());
			}
		}
	}
}
