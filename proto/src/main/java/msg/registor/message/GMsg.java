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

	@ClassField(value = GameProto.NotCard.class, des = "发牌通知")
	public static final int NOT_CARD = CMsg.GAME_TYPE | 3;

	@ClassField(value = GameProto.NotOperation.class, des = "操作通知")
	public static final int NOT_OP = CMsg.GAME_TYPE | 4;

	@ClassField(value = GameProto.ReqOp.class, des = "请求操作")
	public static final int REQ_OP = CMsg.GAME_TYPE | 5;

	@ClassField(value = GameProto.AckOp.class, des = "操作回复")
	public static final int ACK_OP = CMsg.GAME_TYPE | 6;

	@ClassField(value = GameProto.ReqLeaveTable.class, des = "请求离开桌子")
	public static final int REQ_LEAVE = CMsg.GAME_TYPE | 7;

	@ClassField(value = GameProto.AckLeaveTable.class, des = "离开桌子回复")
	public static final int ACK_LEAVE = CMsg.GAME_TYPE | 8;
}
