package msg;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.MessageLite;
import proto.GameProto;
import proto.HallProto;

// BASE_ID_INDEX 以下 的是通用消息
// 发个哪个服务的 用 msgId / BASE_ID_INDEX  得到该服务的类型
// 客户端回复消息都是 大于 BASE_ID_INDEX 对2 取余为0的
public interface MessageHandel {

	int GATE_TYPE = 1;
	int GAME_TYPE = 2;
	int HALL_TYPE = 3;

	int HEART_REQ = 1;//心跳
	int HEART_ACK = 2;//回复
	int REGISTER = 3;//请求注册
	int REGISTER_NOTICE = 4;//注册通知

	int SERVER_REQ = 5;//服务信息请
	int SERVER_ACK = 6;//服务信息回复

	int BASE_ID_INDEX = 10000;

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
		ENTER_TABLE_REQ(20001, GameProto.ReqEnterTable.class),
		ENTER_TABLE_ACK(20002, GameProto.AckEnterTable.class),
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
		REQ_LOGIN(30001, HallProto.ReqLogin.class),
		ACK_LOGIN(30002, HallProto.AckLogin.class),
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
		MessageLite defaultInstance = com.google.protobuf.Internal.getDefaultInstance(clazz);
		if (null == bytes) {
			return defaultInstance.newBuilderForType().build();
		} else {
			return defaultInstance.getParserForType().parseFrom(bytes);
		}
	}
}
