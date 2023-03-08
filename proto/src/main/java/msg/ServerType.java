package msg;

import java.util.HashMap;
import java.util.Map;

public enum ServerType {
	Gate(1, "网关"),
	Game(2, "游戏"),
	Hall(3, "大厅"),
	Router(4, "路由"),
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
}
