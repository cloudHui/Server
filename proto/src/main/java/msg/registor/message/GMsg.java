package msg.registor.message;

import msg.annotation.ClassType;
import msg.registor.enums.MessageTrans;
import proto.GameProto;

/**
 * @author admin
 * @className GameMessageId
 * @description 游戏服务消息
 * @createDate 2025/4/16 10:58
 */
public class GMsg {

	@ClassType(value = GameProto.ReqEnterTable.class, messageTrans = { MessageTrans.GameServer }, des = "请求入桌")
	public static final int REQ_ENTER_TABLE_MSG = CMsg.GAME_TYPE | 1;

	@ClassType(value = GameProto.AckEnterTable.class, messageTrans = { MessageTrans.RobotClient }, des = "入桌回复")
	public static final int ACK_ENTER_TABLE_MSG = CMsg.GAME_TYPE | 2;

	@ClassType(value = GameProto.ReqCreateTable.class, messageTrans = { MessageTrans.GameServer }, des = "请求创建桌子")
	public static final int REQ_CREATE_TABLE_MSG = CMsg.GAME_TYPE | 3;

	@ClassType(value = GameProto.AckCreateTable.class, messageTrans = { MessageTrans.RobotClient, MessageTrans.RoomClient }, des = "创建桌子回复")
	public static final int ACK_CREATE_TABLE_MSG = CMsg.GAME_TYPE | 4;
}
