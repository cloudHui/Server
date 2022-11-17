package msg;

import java.util.HashMap;
import java.util.Map;

import proto.GameProto;
import proto.GateProto;

public interface MsgId {

	int GATE_TYPE = 1;
	int GAME_TYPE = 2;
	int HALL_TYPE = 3;

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
}
