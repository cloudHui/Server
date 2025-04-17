package msg;

import msg.annotation.ClassType;
import proto.HallProto;

/**
 * @author admin
 * @className HallMessageId
 * @description
 * @createDate 2025/4/17 2:47
 */
public class HallMessageId {

	@ClassType(HallProto.ReqLogin.class)
	public static final int REQ_LOGIN_MSG = MessageId.HALL_TYPE | 1;
	@ClassType(HallProto.AckLogin.class)
	public static final int ACK_LOGIN_MSG = MessageId.HALL_TYPE | 2;
	@ClassType(HallProto.ReqJoinClub.class)
	public static final int REQ_JOIN_CLUB_MSG = MessageId.HALL_TYPE | 3;
	@ClassType(HallProto.AckJoinClub.class)
	public static final int ACK_JOIN_CLUB_MSG = MessageId.HALL_TYPE | 4;
}
