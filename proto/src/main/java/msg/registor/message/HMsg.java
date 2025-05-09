package msg.registor.message;

import msg.annotation.ClassType;
import msg.registor.enums.MessageTrans;
import proto.HallProto;

/**
 * @author admin
 * @className HallMessageId
 * @description 大厅服务消息
 * @createDate 2025/4/17 2:47
 */
public class HMsg {

	@ClassType(value = HallProto.ReqLogin.class, messageTrans = { MessageTrans.HallServer }, des = "请求登录")
	public static final int REQ_LOGIN_MSG = CMsg.HALL_TYPE | 1;

	@ClassType(value = HallProto.AckLogin.class, messageTrans = { MessageTrans.RobotClient }, des = "登录回复")
	public static final int ACK_LOGIN_MSG = CMsg.HALL_TYPE | 2;

	@ClassType(value = HallProto.ReqJoinClub.class, messageTrans = { MessageTrans.HallServer }, des = "请求加入工会")
	public static final int REQ_JOIN_CLUB_MSG = CMsg.HALL_TYPE | 3;

	@ClassType(value = HallProto.AckJoinClub.class, messageTrans = { MessageTrans.RobotClient }, des = "加入工会回复")
	public static final int ACK_JOIN_CLUB_MSG = CMsg.HALL_TYPE | 4;
}
