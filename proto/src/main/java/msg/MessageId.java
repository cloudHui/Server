package msg;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Internal;
import com.google.protobuf.MessageLite;
import msg.annotation.ClassType;
import proto.GameProto;
import proto.HallProto;
import proto.RoomProto;

// BASE_ID_INDEX 以下 的是通用消息
// 发个哪个服务的 用 msgId / BASE_ID_INDEX  得到该服务的类型
// 客户端回复消息都是 大于 BASE_ID_INDEX
public interface MessageId {

	int GAME_TYPE = 0x2000;
	int HALL_TYPE = 0x4000;
	int ROOM_TYPE = 0x8000;

	int HEART = 1;//心跳
	int HEART_ACK = 2;//回复
	int REQ_REGISTER = 3;//请求注册
	int ACK_REGISTER = 4;//注册回复
	int REGISTER_NOTICE = 5;//注册通知
	int BREAK_NOTICE = 6;//服务掉线通知


	int REQ_SERVER = 7;//服务信息请
	int ACK_SERVER = 8;//服务信息回复

	int NOT_BREAK = 9;//通知玩家掉线

	int BROAD = 10;//广播

	int BASE_ID_INDEX = 0x1000;

	//Hall
	int REQ_LOGIN_MSG = HALL_TYPE | 1;
	int ACK_LOGIN_MSG = HALL_TYPE | 2;
	int REQ_JOIN_CLUB_MSG = HALL_TYPE | 3;
	int ACK_JOIN_CLUB_MSG = HALL_TYPE | 4;
	enum HallMsg {
		REQ_LOGIN(REQ_LOGIN_MSG, HallProto.ReqLogin.class),
		ACK_LOGIN(ACK_LOGIN_MSG, HallProto.AckLogin.class),
		REQ_JOIN_CLUB(REQ_JOIN_CLUB_MSG, HallProto.ReqJoinClub.class),
		ACK_JOIN_CLUB(ACK_JOIN_CLUB_MSG, HallProto.AckJoinClub.class),
		;
		private final int id;

		private final Class<? extends MessageLite> className;

		private static final Map<Integer, HallMsg> es = new HashMap<>();

		static {
			for (HallMsg msg : values()) {
				es.put(msg.getId(), msg);
			}
		}

		HallMsg(int id, Class<? extends MessageLite> className) {
			this.id = id;
			this.className = className;
		}

		public int getId() {
			return id;
		}

		public Class<? extends MessageLite> getClassName() {
			return className;
		}

		private static Map<Integer, HallMsg> getEs() {
			return es;
		}

		public static HallMsg get(int msgId) {
			return getEs().get(msgId);
		}
	}

	//Room
	int REQ_ROOM_LIST_MSG = ROOM_TYPE | 1;
	int ACK_ROOM_LIST_MSG = ROOM_TYPE | 2;
	enum RoomMsg {
		REQ_ROOM_LIST(REQ_ROOM_LIST_MSG, RoomProto.ReqGetRoomList.class),
		ACK_ROOM_LIST(ACK_ROOM_LIST_MSG, RoomProto.AckGetRoomList.class),
		;
		private final int id;

		private final Class<? extends MessageLite> className;

		private static final Map<Integer, RoomMsg> es = new HashMap<>();

		static {
			for (RoomMsg msg : values()) {
				es.put(msg.getId(), msg);
			}
		}

		RoomMsg(int id, Class<? extends MessageLite> className) {
			this.id = id;
			this.className = className;
		}

		public int getId() {
			return id;
		}

		public Class<? extends MessageLite> getClassName() {
			return className;
		}

		private static Map<Integer, RoomMsg> getEs() {
			return es;
		}

		public static RoomMsg get(int msgId) {
			return getEs().get(msgId);
		}
	}

	static MessageLite getMessageObject(Class<MessageLite> clazz, byte[] bytes) throws Exception {
		MessageLite defaultInstance = Internal.getDefaultInstance(clazz);
		if (null == bytes) {
			return defaultInstance.newBuilderForType().build();
		} else {
			return defaultInstance.getParserForType().parseFrom(bytes);
		}
	}

	/**
	 * 通过消息id获取要转发的服务类型
	 */
	static ServerType getServerTypeByMessageId(int msgId) {
		if ((msgId & MessageId.GAME_TYPE) != 0) {
			return ServerType.Game;
		} else if ((msgId & MessageId.HALL_TYPE) != 0) {
			return ServerType.Hall;
		} else if ((msgId & MessageId.ROOM_TYPE) != 0) {
			return ServerType.Room;
		}
		return null;
	}
}
