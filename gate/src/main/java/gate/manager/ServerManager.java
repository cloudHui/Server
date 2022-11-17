package gate.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import gate.client.GateClient;
import msg.ServerType;
import utils.utils.RandomUtils;

/**
 * gate 和其他服务链接管理
 */
public class ServerManager {

	private static ServerManager instance;

	private Map<ServerType, Map<Integer, GateClient>> serverMap = new ConcurrentHashMap<>();

	static {
		instance = new ServerManager();
	}

	private ServerManager() {
	}

	public ServerManager getInstance() {
		return instance;
	}

	/**
	 * 添加服务链接
	 *
	 * @param serverType 服务类型
	 * @param client     链接
	 */
	public void addServerClient(ServerType serverType, GateClient client) {
		serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>()).put((int) client.getId(), client);
	}

	/**
	 * 移除服务链接
	 *
	 * @param serverType 服务类型
	 * @param client     链接
	 */
	public void removeServerClient(ServerType serverType, GateClient client) {
		serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>()).remove((int) client.getId());
	}

	/**
	 * 获取制动id类型的服务链接
	 *
	 * @param serverType 服务类型
	 * @param clientId   链接id
	 */
	public GateClient getServerClient(ServerType serverType, int clientId) {
		return serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>()).get(clientId);
	}

	/**
	 * 获取随机类型服务链接
	 *
	 * @param serverType 服务类型
	 */
	public GateClient getServerClient(ServerType serverType) {
		GateClient client = null;
		Map<Integer, GateClient> serverClient = serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>());
		if (!serverClient.isEmpty()) {
			List<Integer> list = new ArrayList<>(serverClient.keySet());
			int serverId = list.get(RandomUtils.Random(0, list.size()));
			return serverClient.get(serverId);
		}
		return client;
	}
}
