package msg;


/**
 * 消息注册转化类型
 */
public enum MessageTrans {
	CenterServer(ServerType.Center, MessageId.SERVER, "注册中心服务器"),

	GateClient(ServerType.Gate, MessageId.CLIENT, "网关客户端"),

	GameServer(ServerType.Game, MessageId.SERVER, "游戏服务器"),
	GameClient(ServerType.Game, MessageId.CLIENT, "游戏客户端"),

	HallServer(ServerType.Hall, MessageId.SERVER, "大厅服务器"),
	HallClient(ServerType.Hall, MessageId.CLIENT, "大厅客户端"),

	RobotServer(ServerType.Robot, MessageId.SERVER, "机器人服务器"),

	RoomServer(ServerType.Room, MessageId.SERVER, "房间服务器"),
	RoomClient(ServerType.Room, MessageId.CLIENT, "房间客户端"),
	;

	MessageTrans(ServerType serverType, int serverClient, String desc) {
		this.serverType = serverType;
		this.desc = desc;
		this.serverClient = serverClient;
	}

	private final ServerType serverType;

	private final String desc;

	private final int serverClient;

	public ServerType getServerType() {
		return serverType;
	}

	public int getServerClient() {
		return serverClient;
	}

	@Override
	public String toString() {
		return "ServerTypes{" +
				"serverType=" + serverType +
				", desc='" + desc + '\'' +
				", serverClient=" + serverClient +
				'}';
	}
}
