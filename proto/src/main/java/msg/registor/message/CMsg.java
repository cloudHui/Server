package msg.registor.message;

import msg.annotation.ClassField;
import msg.annotation.ClassType;
import proto.GateProto;
import proto.ModelProto;

/**
 * BASE_ID_INDEX 以下 的是通用消息
 * 发个哪个服务的 用 msgId / BASE_ID_INDEX  得到该服务的类型
 * 客户端回复消息都是 大于 BASE_ID_INDEX
 */
@ClassType
public class CMsg {

	public static final int GAME_TYPE = 0x2000;
	public static final int HALL_TYPE = 0x4000;
	public static final int ROOM_TYPE = 0x8000;

	@ClassField(value = ModelProto.ReqHeart.class, des = "心跳")
	public static final int HEART = 1;

	@ClassField(value = ModelProto.AckHeart.class, des = "心跳回复")
	public static final int HEART_ACK = 2;

	@ClassField(value = ModelProto.ReqRegister.class, des = "请求注册")
	public static final int REQ_REGISTER = 3;

	@ClassField(value = ModelProto.AckRegister.class, des = "注册回复")
	public static final int ACK_REGISTER = 4;

	@ClassField(value = ModelProto.NotRegisterInfo.class, des = "注册通知")
	public static final int REGISTER_NOTICE = 5;

	@ClassField(value = ModelProto.NotServerBreak.class, des = "服务掉线通知")
	public static final int BREAK_NOTICE = 6;

	@ClassField(value = ModelProto.ReqServerInfo.class, des = "服务信息请求")
	public static final int REQ_SERVER = 7;

	@ClassField(value = ModelProto.AckServerInfo.class, des = "服务信息回复")
	public static final int ACK_SERVER = 8;

	@ClassField(value = ModelProto.NotBreak.class, des = "通知玩家掉线")
	public static final int NOT_BREAK = 9;

	@ClassField(value = ModelProto.NotRegisterClient.class, des = "通知中心玩家登录")
	public static final int NOT_LINK = 10;

	@ClassField(value = GateProto.BroadCast.class, des = "广播")
	public static final int BROAD = 11;

	public static final int BASE_ID_INDEX = 0x1000;
}
