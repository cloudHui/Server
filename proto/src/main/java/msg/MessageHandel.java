package msg;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.MessageLite;
import proto.GameProto;
import proto.GateProto;
import proto.ModelProto;

public interface MessageHandel {

	int GATE_TYPE = 1;
	int GAME_TYPE = 2;
	int HALL_TYPE = 3;

	int HEART_REQ = 1;
	int HEART_ACK = 2;
	int REGISTER = 3;

	int BASE_ID_INDEX = 10000;

	enum GateMsg {
		LOGIN_REQ(10001, GateProto.ReqLogin.class),
		LOGIN_ACK(10002, GateProto.AckLogin.class),
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

		public static Map<Integer, GateMsg> getEs() {
			return es;
		}
	}

	enum GameMsg {
		ENTER_TABLE_REQ(20001, GameProto.ReqEnterTable.class),
		ENTER_TABLE_ACK(20002, GameProto.AckEnterTable.class),
		;
		private int id;

		private Class className;

		private static Map<Integer,
				GameMsg> es = new HashMap<>();

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

		public static Map<Integer, GameMsg> getEs() {
			return es;
		}
	}

	enum CenterMsg {
		SERVER_REQ(50001, ModelProto.ReqServerInfo.class),
		SERVER_ACK(50002, ModelProto.AckServerInfo.class),
		;
		private int id;

		private Class className;

		private static Map<Integer, CenterMsg> es = new HashMap<>();

		static {
			for (CenterMsg msg : values()) {
				es.put(msg.getId(), msg);
			}
		}

		CenterMsg(int id, Class className) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public Class getClassName() {
			return className;
		}

		private static Map<Integer, CenterMsg> getEs() {
			return es;
		}

		public static CenterMsg get(int msgId) {
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
