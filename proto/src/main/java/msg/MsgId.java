package msg;

import java.util.HashMap;
import java.util.Map;

public interface MsgId {

	int GATE_TYPE = 1;
	int GAME_TYPE = 2;
	int HALL_TYPE = 3;
	int ROOM_TYPE = 4;

	int BASE_ID_INDEX = 10000;

	enum GateMsg {
		LOGIN_REQ(10001, Gate.CTGLogin.class),
		LOGIN_ACK(10002, Gate.GTCLogin.class),
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
		ENTER_TABLE_REQ(20001, Game.CTGEnterTable.class),
		ENTER_TABLE_ACK(20002, Game.GTCEnterTable.class),
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

		public static Map<Integer, GameMsg> getEs() {
			return es;
		}
	}
}
