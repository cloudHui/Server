package msg.registor.message;

import msg.annotation.ClassField;
import msg.annotation.ClassType;
import proto.ServerProto;

/**
 * @author admin
 * @className ServerMessageId
 * @description 服务间消息
 * @createDate 2025/4/17 3:04
 */
@ClassType
public class SMsg {

	@ClassField(value = ServerProto.ReqCreateGameTable.class, des = "请求创建桌子")
	public static final int REQ_CREATE_TABLE_MSG = CMsg.SERVER_TYPE | 1;

	@ClassField(value = ServerProto.AckCreateGameTable.class, des = "创建桌子回复")
	public static final int ACK_CREATE_TABLE_MSG = CMsg.SERVER_TYPE | 2;

	@ClassField(value = ServerProto.ReqRoomTable.class, des = "请求获取桌子列表")
	public static final int REQ_GET_TABLE_MSG = CMsg.SERVER_TYPE | 3;

	@ClassField(value = ServerProto.AckRoomTable.class, des = "获取桌子列表回复")
	public static final int ACK_GET_TABLE_MSG = CMsg.SERVER_TYPE | 4;
}
