package msg.annotation;

import msg.ServerType;

/**
 * @author admin
 * @className ServerType
 * @description
 * @createDate 2025/4/17 6:29
 */
public class ServerTypes {

	public ServerType serverType;

	public int[] type;

	public ServerTypes(ServerType serverType, int[] type) {
		this.serverType = serverType;
		this.type = type;
	}

	public ServerType getServerType() {
		return serverType;
	}

	public int[] getType() {
		return type;
	}
}
