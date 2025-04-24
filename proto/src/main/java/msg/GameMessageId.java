package msg;

import msg.annotation.ClassType;
import proto.GameProto;

/**
 * @author admin
 * @className GameMessageId
 * @description
 * @createDate 2025/4/16 10:58
 */
public class GameMessageId {

	@ClassType(value = GameProto.ReqEnterTable.class, messageTrans = { MessageTrans.GameServer }, des = "请求入桌")
	public static final int REQ_ENTER_TABLE_MSG = MessageId.GAME_TYPE | 1;

	@ClassType(value = GameProto.AckEnterTable.class, messageTrans = { MessageTrans.RobotClient }, des = "入桌回复")
	public static final int ACK_ENTER_TABLE_MSG = MessageId.GAME_TYPE | 2;
}
