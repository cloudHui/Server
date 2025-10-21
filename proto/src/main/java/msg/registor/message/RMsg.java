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

	@ClassField(value = RoomProto.ReqJoinRoomTable.class, des = "请求加入桌子")
	public static final int REQ_JOIN_ROOM_TABLE_MSG = CMsg.ROOM_TYPE | 1;

	@ClassField(value = RoomProto.AckJoinRoomTable.class, des = "加入桌子回复")
	public static final int ACK_JOIN_ROOM_TABLE_MSG = CMsg.ROOM_TYPE | 2;

}
