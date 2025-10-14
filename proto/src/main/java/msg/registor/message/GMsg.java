package msg.registor.message;

import msg.annotation.ClassField;
import msg.annotation.ClassType;
import proto.GameProto;

/**
 * @author admin
 * @className GameMessageId
 * @description 游戏服务消息
 * @createDate 2025/4/16 10:58
 */
@ClassType
public class GMsg {

	@ClassField(value = GameProto.ReqEnterTable.class, des = "请求入桌")
	public static final int REQ_ENTER_TABLE_MSG = CMsg.GAME_TYPE | 1;

	@ClassField(value = GameProto.AckEnterTable.class, des = "入桌回复")
	public static final int ACK_ENTER_TABLE_MSG = CMsg.GAME_TYPE | 2;
}
