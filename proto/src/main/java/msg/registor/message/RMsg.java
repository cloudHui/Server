package msg.registor.message;

import msg.annotation.ClassField;
import msg.annotation.ClassType;
import proto.RoomProto;

/**
 * @author admin
 * @className RoomMessageId
 * @description 房间服务消息
 * @createDate 2025/4/17 3:04
 */
@ClassType
public class RMsg {

	@ClassField(value = RoomProto.ReqGetRoomList.class, des = "请求房间列表")
	public static final int REQ_ROOM_LIST_MSG = CMsg.ROOM_TYPE | 1;

	@ClassField(value = RoomProto.AckGetRoomList.class, des = "房间列表回复")
	public static final int ACK_ROOM_LIST_MSG = CMsg.ROOM_TYPE | 2;

	@ClassField(value = RoomProto.ReqCreateRoomTable.class, des = "请求创建桌子")
	public static final int REQ_CREATE_ROOM_TABLE_MSG = CMsg.ROOM_TYPE | 3;

}
