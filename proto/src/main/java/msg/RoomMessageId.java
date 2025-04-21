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

	@ClassType(value = RoomProto.ReqGetRoomList.class, des = "请求房间列表")
	public static final int REQ_ROOM_LIST_MSG = MessageId.ROOM_TYPE | 1;

	@ClassType(value = RoomProto.AckGetRoomList.class, des = "房间列表回复")
	public static final int ACK_ROOM_LIST_MSG = MessageId.ROOM_TYPE | 2;

	@ClassType(value = RoomProto.ReqGetRoomList.class, des = "请求创建桌子")
	public static final int REQ_CREATE_TABLE_MSG = MessageId.ROOM_TYPE | 3;

}
