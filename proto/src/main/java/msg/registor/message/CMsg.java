package msg.registor.message;

import msg.annotation.ClassType;
import msg.registor.enums.MessageTrans;
import proto.GateProto;
import proto.ModelProto;

/**
 * BASE_ID_INDEX 以下 的是通用消息
 * 发个哪个服务的 用 msgId / BASE_ID_INDEX  得到该服务的类型
 * 客户端回复消息都是 大于 BASE_ID_INDEX
 */
public class CMsg {

	/**
	 * 服务端
	 */
	public static final int SERVER = 1;
	/**
	 * 客户端
	 */
	public static final int CLIENT = 2;

	public static final int GAME_TYPE = 0x2000;
	public static final int HALL_TYPE = 0x4000;
	public static final int ROOM_TYPE = 0x8000;

	@ClassType(value = ModelProto.ReqHeart.class,
			messageTrans = { MessageTrans.GameServer, MessageTrans.CenterServer, MessageTrans.RoomServer, MessageTrans.HallServer },
			des = "心跳")
	public static final int HEART = 1;

	@ClassType(value = ModelProto.AckHeart.class,
			messageTrans = { MessageTrans.GameClient, MessageTrans.GateClient, MessageTrans.HallClient, MessageTrans.RoomClient },
			des = "心跳回复")
	public static final int HEART_ACK = 2;

	@ClassType(value = ModelProto.ReqRegister.class,
			messageTrans = { MessageTrans.GameServer, MessageTrans.CenterServer, MessageTrans.HallServer, MessageTrans.RoomServer },
			des = "请求注册")
	public static final int REQ_REGISTER = 3;

	@ClassType(value = ModelProto.AckRegister.class,
			messageTrans = { MessageTrans.GameClient, MessageTrans.GateClient, MessageTrans.HallClient, MessageTrans.RoomClient },
			des = "注册回复")
	public static final int ACK_REGISTER = 4;

	@ClassType(value = ModelProto.NotRegisterInfo.class,
			messageTrans = { MessageTrans.GateClient, MessageTrans.HallClient, MessageTrans.RoomClient },
			des = "注册通知")
	public static final int REGISTER_NOTICE = 5;

	@ClassType(value = ModelProto.NotServerBreak.class,
			messageTrans = { MessageTrans.GameServer, MessageTrans.HallServer, MessageTrans.RoomServer },
			des = "服务掉线通知")
	public static final int BREAK_NOTICE = 6;

	@ClassType(value = ModelProto.ReqServerInfo.class, messageTrans = { MessageTrans.CenterServer }, des = "服务信息请求")
	public static final int REQ_SERVER = 7;

	@ClassType(value = ModelProto.AckServerInfo.class,
			messageTrans = { MessageTrans.GateClient, MessageTrans.HallClient, MessageTrans.RoomClient },
			des = "服务信息回复")
	public static final int ACK_SERVER = 8;

	@ClassType(value = ModelProto.NotBreak.class,
			messageTrans = { MessageTrans.GameServer, MessageTrans.HallServer, MessageTrans.RoomServer },
			des = "通知玩家掉线")
	public static final int NOT_BREAK = 9;

	@ClassType(value = GateProto.BroadCast.class, messageTrans = { MessageTrans.GateClient }, des = "广播")
	public static final int BROAD = 10;

	public static final int BASE_ID_INDEX = 0x1000;
}
