package msg;

import msg.annotation.ClassType;
import proto.RoomProto;

/**
 * @author admin
 * @className RoomMessageId
 * @description
 * @createDate 2025/4/17 3:04
 */
public class RoomMessageId {

	@ClassType(RoomProto.ReqGetRoomList.class)
	public static final int REQ_ROOM_LIST_MSG = MessageId.ROOM_TYPE | 1;
	@ClassType(RoomProto.AckGetRoomList.class)
	public static final int ACK_ROOM_LIST_MSG = MessageId.ROOM_TYPE | 2;
}
