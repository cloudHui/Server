package msg.registor.enums;


import msg.registor.message.CMsg;

/**
 * 消息注册转化类型
 */
public enum MessageTrans {
	CenterServer(ServerType.Center, CMsg.SERVER, "注册中心服务器"),

	GateClient(ServerType.Gate, CMsg.CLIENT, "网关客户端"),

	GameServer(ServerType.Game, CMsg.SERVER, "游戏服务器"),
	GameClient(ServerType.Game, CMsg.CLIENT, "游戏客户端"),

	HallServer(ServerType.Hall, CMsg.SERVER, "大厅服务器"),
	HallClient(ServerType.Hall, CMsg.CLIENT, "大厅客户端"),

	RobotClient(ServerType.Robot, CMsg.CLIENT, "机器人客户端"),

	RoomServer(ServerType.Room, CMsg.SERVER, "房间服务器"),
	RoomClient(ServerType.Room, CMsg.CLIENT, "房间客户端"),
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
