package msg;

import java.util.HashMap;
import java.util.Map;

public interface MsgId {

	int gateIdIndex = 10000;
	int hallIdIndex = 20000;
	int roomIdIndex = 30000;
	int gameIdIndex = 40000;
	int routerIdIndex = 50000;

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
