package gate.manager;

/**
 * gate 和其他服务链接管理
 */
public class ServerManager {

	private static ServerManager instance;

	static {
		instance = new ServerManager();
	}

	private ServerManager() {
	}

	public ServerManager getInstance() {
		return instance;
	}
}
