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

	@ClassType(value = HallProto.ReqLogin.class, des = "请求登录")
	public static final int REQ_LOGIN_MSG = MessageId.HALL_TYPE | 1;
	@ClassType(value = HallProto.AckLogin.class, des = "登录回复")
	public static final int ACK_LOGIN_MSG = MessageId.HALL_TYPE | 2;
	@ClassType(value = HallProto.ReqJoinClub.class, des = "请求加入工会")
	public static final int REQ_JOIN_CLUB_MSG = MessageId.HALL_TYPE | 3;
	@ClassType(value = HallProto.AckJoinClub.class, des = "加入工会回复")
	public static final int ACK_JOIN_CLUB_MSG = MessageId.HALL_TYPE | 4;
}
