package msg;


import java.util.Arrays;

/**
 * 消息注册转化类型
 */
public enum MessageTrans {
	CenterServer(ServerType.Center, new int[] { MessageId.SERVER }, "注册中心服务器"),

	GateClient(ServerType.Gate, new int[] { MessageId.CLIENT }, "网关客户端"),

	GameServer(ServerType.Game, new int[] { MessageId.SERVER }, "游戏服务器"),
	GameClient(ServerType.Game, new int[] { MessageId.CLIENT }, "游戏客户端"),
	GameServerClient(ServerType.Game, new int[] { MessageId.SERVER, MessageId.CLIENT }, "游戏服务器客户端"),

	HallServer(ServerType.Hall, new int[] { MessageId.SERVER }, "大厅服务器"),
	HallClient(ServerType.Hall, new int[] { MessageId.CLIENT }, "大厅客户端"),
	HallServerClient(ServerType.Hall, new int[] { MessageId.SERVER, MessageId.CLIENT }, "大厅服务器客户端"),

	RobotServer(ServerType.Robot, new int[] { MessageId.SERVER }, "机器人服务器"),
	RobotClient(ServerType.Robot, new int[] { MessageId.CLIENT }, "机器人客户端"),
	RobotServerClient(ServerType.Robot, new int[] { MessageId.SERVER, MessageId.CLIENT }, "机器人服务器客户端"),

	RoomServer(ServerType.Room, new int[] { MessageId.SERVER }, "房间服务器"),
	RoomClient(ServerType.Room, new int[] { MessageId.CLIENT }, "房间客户端"),
	RoomServerClient(ServerType.Room, new int[] { MessageId.SERVER, MessageId.CLIENT }, "房间服务器客户端"),
	;

	MessageTrans(ServerType serverType, int[] serverClient, String desc) {
		this.serverType = serverType;
		this.desc = desc;
		this.serverClient = serverClient;
	}

	private final ServerType serverType;

	private final String desc;

	private final int[] serverClient;

	public ServerType getServerType() {
		return serverType;
	}

	public int[] getServerClient() {
		return serverClient;
	}

	@Override
	public String toString() {
		return "ServerTypes{" +
				"serverType=" + serverType +
				", desc='" + desc + '\'' +
				", serverClient=" + Arrays.toString(serverClient) +
				'}';
	}
}
