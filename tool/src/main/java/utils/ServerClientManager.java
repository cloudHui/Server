package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import msg.ServerType;
import net.client.handler.ClientHandler;
import utils.utils.RandomUtils;

/**
 * 服务链接管理
 */
public class ServerClientManager {

	private Map<ServerType, Map<Integer, ClientHandler>> serverMap = new ConcurrentHashMap<>();

	/**
	 * 添加服务链接
	 *
	 * @param serverType 服务类型
	 * @param client     链接
	 * @param serverId   服务id
	 */
	public void addServerClient(ServerType serverType, ClientHandler client, int serverId) {
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
	public ClientHandler getServerClient(ServerType serverType, int serverId) {
		return serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>()).get(serverId);
	}

	/**
	 * 获取随机类型服务链接
	 *
	 * @param serverType 服务类型
	 */
	public ClientHandler getServerClient(ServerType serverType) {
		Map<Integer, ClientHandler> serverClient = serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>());
		if (!serverClient.isEmpty()) {
			List<Integer> list = new ArrayList<>(serverClient.keySet());
			int serverId = list.get(RandomUtils.randomRange(list.size()));
			return serverClient.get(serverId);
		}
		return null;
	}

	/**
	 * 获取所有类型服务链接
	 *
	 * @param serverType 服务类型
	 */
	public List<ClientHandler> getAllTypeServer(ServerType serverType) {
		return new ArrayList<>(serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>()).values());
	}
}
