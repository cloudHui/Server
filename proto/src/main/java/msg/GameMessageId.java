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

	@ClassType(value = GameProto.ReqEnterTable.class)
	public static final int REQ_ENTER_TABLE_MSG = MessageId.GAME_TYPE | 1;

	@ClassType(GameProto.AckEnterTable.class)
	public static final int ACK_ENTER_TABLE_MSG = MessageId.GAME_TYPE | 2;
}
