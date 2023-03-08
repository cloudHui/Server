package gate.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import gate.client.GateClient;
import msg.ServerType;
import utils.utils.RandomUtils;

/**
 * 服务链接管理
 */
public class ServerManager {

	private static ServerManager instance;

	private Map<ServerType, Map<Integer, GateClient>> serverMap = new ConcurrentHashMap<>();

	static {
		instance = new ServerManager();
	}

	private ServerManager() {
	}

	public static ServerManager getInstance() {
		return instance;
	}

	/**
	 * 添加服务链接
	 *
	 * @param serverType 服务类型
	 * @param client     链接
	 * @param serverId   服务id
	 */
	public void addServerClient(ServerType serverType, GateClient client, int serverId) {
		serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>()).put(serverId, client);
	}

	/**
	 * 移除服务链接
	 *
	 * @param serverType 服务类型
	 * @param serverId   服务id
	 */
	public void removeServerClient(ServerType serverType, int serverId) {
		serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>()).remove(serverId);
	}

	/**
	 * 获取指定服务id 类型的服务链接
	 *
	 * @param serverType 服务类型
	 * @param serverId   链接id
	 */
	public GateClient getServerClient(ServerType serverType, int serverId) {
		return serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>()).get(serverId);
	}

	/**
	 * 获取随机类型服务链接
	 *
	 * @param serverType 服务类型
	 */
	public GateClient getServerClient(ServerType serverType) {
		Map<Integer, GateClient> serverClient = serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>());
		if (!serverClient.isEmpty()) {
			List<Integer> list = new ArrayList<>(serverClient.keySet());
			int serverId = list.get(RandomUtils.Random(0, list.size()));
			return serverClient.get(serverId);
		}
		return null;
	}
}
