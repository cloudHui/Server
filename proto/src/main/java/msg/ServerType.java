package msg;

public enum ServerType {
	Gate(1,"网关"),
	Game(2,"游戏"),
	Hall(3,"大厅"),
	Router(4,"路由"),
	;

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
}
