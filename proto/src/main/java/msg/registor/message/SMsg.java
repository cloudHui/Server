package msg.registor.message;

import msg.annotation.ClassField;
import msg.annotation.ClassType;
import proto.ServerProto;

/**
 * 服务间消息（lobby ↔ game）
 */
@ClassType
public class SMsg {

	@ClassField(value = ServerProto.ReqCreateGameTable.class, des = "lobby向game请求创建桌子")
	public static final int REQ_CREATE_TABLE_MSG = CMsg.SERVER_TYPE | 1;

	@ClassField(value = ServerProto.AckCreateGameTable.class, des = "lobby向game创建桌子回复")
	public static final int ACK_CREATE_TABLE_MSG = CMsg.SERVER_TYPE | 2;

	@ClassField(value = ServerProto.ReqRoomTables.class, des = "lobby向game请求桌子列表")
	public static final int REQ_ROOM_TABLES_MSG = CMsg.SERVER_TYPE | 5;

	@ClassField(value = ServerProto.AckRoomTables.class, des = "game返回桌子列表")
	public static final int ACK_ROOM_TABLES_MSG = CMsg.SERVER_TYPE | 6;

	@ClassField(value = ServerProto.NotTableDestroyed.class, des = "game通知lobby桌子销毁")
	public static final int NOT_TABLE_DESTROYED_MSG = CMsg.SERVER_TYPE | 7;

	@ClassField(value = ServerProto.NotTablePlayerLeft.class, des = "game通知lobby玩家离桌")
	public static final int NOT_TABLE_PLAYER_LEFT_MSG = CMsg.SERVER_TYPE | 8;
}
