package robot.connect.handel;

import com.google.protobuf.Message;
import msg.registor.message.RMsg;
import msg.registor.enums.ServerType;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.RoomProto;
import robot.Robot;

/**
 * 获取房间回复
 */
@ProcessType(RMsg.ACK_ROOM_LIST_MSG)
public class AckGetRoomListHandler implements Handler {

	private final static Logger logger = LoggerFactory.getLogger(AckGetRoomListHandler.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message ack, int mapId, long sequence) {
		logger.error("[get room ack:{}]", ack.toString());
		RoomProto.AckGetRoomList rooms = (RoomProto.AckGetRoomList) ack;
		if (rooms.getRoomListCount() > 0) {
			RoomProto.Room room = rooms.getRoomList(0);
			RoomProto.ReqCreateTable.Builder createTable = RoomProto.ReqCreateTable.newBuilder();
			createTable.setConfigTypeId(room.getConfigTypeId());
			Robot.getInstance().getClientSendMessage(ServerType.Room, RMsg.REQ_CREATE_TABLE_MSG, createTable.build());
		}
		return true;
	}
}
