package msg;

import java.util.HashMap;
import java.util.Map;

public enum ServerType {
	Gate(1, "网关"),
	Game(2, "游戏"),
	Hall(3, "大厅"),
	Center(4, "注册中心"),
	Robot(5, "机器人"),
	;

	private static Map<Integer, ServerType> es = new HashMap<>();

	static {
		for (ServerType type : values()) {
			es.put(type.getServerType(), type);
		}
	}

	ServerType(int serverType, String desc) {
		this.serverType = serverType;
		this.desc = desc;
	}

	private int serverType;

	private String desc;

	public int getServerType() {
		return serverType;
	}

	public String getDesc() {
		return desc;
	}

	private static Map<Integer, ServerType> getEs() {
		return es;
	}

	public static ServerType get(int serverType) {
		return getEs().get(serverType);
	}

	@Override
	public String toString() {
		return "ServerType{" +
				"serverType=" + serverType +
				", desc='" + desc + '\'' +
				'}';
	}
}
