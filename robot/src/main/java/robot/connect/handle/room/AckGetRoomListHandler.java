package robot.connect.handle.room;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import msg.registor.message.RMsg;
import net.connect.handle.ConnectHandler;
import proto.RoomProto;
import robot.connect.ConnectProcessor;
import utils.manager.ConnectHandle;
import utils.manager.HandleManager;

/**
 * 获取房间回复
 */
@ProcessClass(RoomProto.AckGetRoomList.class)
public class AckGetRoomListHandler implements ConnectHandle {

	@Override
	public void handle(Message message, ConnectHandler serverClient, int sequence) {
		if (message instanceof RoomProto.AckGetRoomList) {
			RoomProto.AckGetRoomList rooms = (RoomProto.AckGetRoomList) message;
			LOGGER.error("AckGetRoomList:{}", rooms.toString());
			if (rooms.getRoomListCount() > 0) {
				RoomProto.Room room = rooms.getRoomList(0);
				RoomProto.ReqJoinRoomTable.Builder createTable = RoomProto.ReqJoinRoomTable.newBuilder();
				createTable.setRoomId(room.getRoomId());
				HandleManager.sendMsg(RMsg.REQ_JOIN_ROOM_TABLE_MSG, createTable.build(), serverClient, ConnectProcessor.PARSER);
			}
		}
	}
}
