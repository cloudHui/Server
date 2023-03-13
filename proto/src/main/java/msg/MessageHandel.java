package msg;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Internal;
import com.google.protobuf.MessageLite;
import proto.GameProto;
import proto.HallProto;

// BASE_ID_INDEX 以下 的是通用消息
// 发个哪个服务的 用 msgId / BASE_ID_INDEX  得到该服务的类型
// 客户端回复消息都是 大于 BASE_ID_INDEX
public interface MessageHandel {

	int GAME_TYPE = 0x2000;
	int HALL_TYPE = 0x4000;

	int HEART = 1;//心跳
	int HEART_ACK = 2;//回复
	int REQ_REGISTER = 3;//请求注册
	int ACK_REGISTER = 4;//注册回复
	int REGISTER_NOTICE = 5;//注册通知

	int REQ_SERVER = 7;//服务信息请
	int ACK_SERVER = 8;//服务信息回复

	int NOT_BREAK = 9;//通知玩家掉线

	int BASE_ID_INDEX = 0x1000;

	enum GateMsg {
		;
		private int id;

		private Class className;

		private static Map<Integer, GateMsg> es = new HashMap<>();

		static {
			for (GateMsg msg : values()) {
				es.put(msg.getId(), msg);
			}
		}

		GateMsg(int id, Class className) {
			this.id = id;
			this.className = className;
		}

		public int getId() {
			return id;
		}

		public Class getClassName() {
			return className;
		}

		private static Map<Integer, GateMsg> getEs() {
			return es;
		}

		public static GateMsg get(int msgId) {
			return getEs().get(msgId);
		}
	}

	enum GameMsg {
		ENTER_TABLE_REQ(GAME_TYPE | 1, GameProto.ReqEnterTable.class),
		ENTER_TABLE_ACK(GAME_TYPE | 2, GameProto.AckEnterTable.class),
		;
		private int id;

		private Class className;

		private static Map<Integer, GameMsg> es = new HashMap<>();

		static {
			for (GameMsg msg : values()) {
				es.put(msg.getId(), msg);
			}
		}

		GameMsg(int id, Class className) {
			this.id = id;
			this.className = className;
		}

		public int getId() {
			return id;
		}

		public Class getClassName() {
			return className;
		}

		private static Map<Integer, GameMsg> getEs() {
			return es;
		}

		public static GameMsg get(int msgId) {
			return getEs().get(msgId);
		}
	}

	enum HallMsg {
		REQ_LOGIN(HALL_TYPE | 1, HallProto.ReqLogin.class),
		ACK_LOGIN(HALL_TYPE | 2, HallProto.AckLogin.class),
		;
		private int id;

		private Class className;

		private static Map<Integer, HallMsg> es = new HashMap<>();

		static {
			for (HallMsg msg : values()) {
				es.put(msg.getId(), msg);
			}
		}

		HallMsg(int id, Class className) {
			this.id = id;
			this.className = className;
		}

		public int getId() {
			return id;
		}

		public Class getClassName() {
			return className;
		}

		private static Map<Integer, HallMsg> getEs() {
			return es;
		}

		public static HallMsg get(int msgId) {
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
}
