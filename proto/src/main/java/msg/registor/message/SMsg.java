package msg.registor.message;

import msg.annotation.ClassField;
import msg.annotation.ClassType;
import proto.ServerProto;

/**
 * @author admin
 * @className RoomMessageId
 * @description 通用服务消息
 * @createDate 2025/4/17 3:04
 */
@ClassType
public class SMsg {

	@ClassField(value = ServerProto.ReqCreateGameTable.class, des = "请求创建桌子")
	public static final int REQ_CREATE_TABLE_MSG = CMsg.SERVER_TYPE | 1;

	@ClassField(value = ServerProto.AckCreateGameTable.class, des = "请求创建桌子回复")
	public static final int ACK_CREATE_TABLE_MSG = CMsg.SERVER_TYPE | 2;

}
