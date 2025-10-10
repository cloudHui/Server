package msg.registor.message;

import msg.annotation.ClassField;
import msg.annotation.ClassType;
import proto.HallProto;

/**
 * @author admin
 * @className HallMessageId
 * @description 大厅服务消息
 * @createDate 2025/4/17 2:47
 */
@ClassType
public class HMsg {

	@ClassField(value = HallProto.ReqLogin.class, des = "请求登录")
	public static final int REQ_LOGIN_MSG = CMsg.HALL_TYPE | 1;

	@ClassField(value = HallProto.AckLogin.class, des = "登录回复")
	public static final int ACK_LOGIN_MSG = CMsg.HALL_TYPE | 2;

	@ClassField(value = HallProto.ReqJoinClub.class, des = "请求加入工会")
	public static final int REQ_JOIN_CLUB_MSG = CMsg.HALL_TYPE | 3;

	@ClassField(value = HallProto.AckJoinClub.class, des = "加入工会回复")
	public static final int ACK_JOIN_CLUB_MSG = CMsg.HALL_TYPE | 4;
}
