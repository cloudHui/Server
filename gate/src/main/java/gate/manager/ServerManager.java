package gate.manager;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import gate.Gate;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import msg.MessageHandel;
import msg.ServerType;
import net.client.event.RegisterEvent;
import net.connect.TCPConnect;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.utils.RandomUtils;

/**
 * 服务链接管理
 */
public class ServerManager {
	private final static Logger logger = LoggerFactory.getLogger(ServerManager.class);

	private static ServerManager instance;

	private Map<ServerType, Map<Integer, TCPConnect>> serverMap = new ConcurrentHashMap<>();

	static {
		instance = new ServerManager();
	}

	private ServerManager() {
	}

	public static ServerManager getInstance() {
		return instance;
	}

	private final EventLoopGroup workerGroup = new NioEventLoopGroup();

	/**
	 * 添加服务链接
	 *
	 * @param serverType 服务类型
	 * @param client     链接
	 * @param serverId   服务id
	 */
	public void addServerClient(ServerType serverType, TCPConnect client, int serverId) {
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
	public TCPConnect getServerClient(ServerType serverType, int serverId) {
		return serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>()).get(serverId);
	}

	/**
	 * 获取随机类型服务链接
	 *
	 * @param serverType 服务类型
	 */
	public TCPConnect getServerClient(ServerType serverType) {
		Map<Integer, TCPConnect> serverClient = serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>());
		if (!serverClient.isEmpty()) {
			List<Integer> list = new ArrayList<>(serverClient.keySet());
			int serverId = list.get(RandomUtils.Random(0, list.size()));
			return serverClient.get(serverId);
		}
		return null;
	}

	public TCPConnect connect(int serverType, String ip, int port, Transfer transfer, Parser parser, Handlers handlers) {
		return connect(serverType, new InetSocketAddress(ip, port), transfer, parser, handlers);
	}

	public TCPConnect connect(int serverType, SocketAddress socketAddress, Transfer transfer, Parser parser, Handlers handlers) {
		return connect(serverType,
				socketAddress,
				transfer,
				parser,
				handlers,
				(RegisterEvent<TCPConnect>) c -> {
					ModelProto.ReqRegisterNotice.Builder notice = ModelProto.ReqRegisterNotice.newBuilder();
					ModelProto.ServerInfo.Builder server = ModelProto.ServerInfo.newBuilder();
					server.setServerType(ServerType.Gate.getServerType());
					server.setServerId(Gate.getInstance().getServerId());
					server.setIpConfig(Gate.getInstance().getIp() + ":" + Gate.getInstance().getPort());
					notice.setServerInfo(server.build());


					c.sendMessage(MessageHandel.REGISTER, notice.build(), null, 10)
							.whenComplete((BiConsumer<ModelProto.AckRegisterNotice, Exception>) (r, e) -> {
								InetSocketAddress s = (InetSocketAddress) socketAddress;
								if (null != e) {
									logger.error("ERROR! failed for send register message to {}:{}",
											s.getAddress().getHostAddress(), s.getPort(), e);
								} else {
									ModelProto.ServerInfo serverInfo = r.getServerInfo();
									c.setServerId(serverInfo.getServerId());

									addServerClient(ServerType.get(serverInfo.getServerType()), c, serverInfo.getServerId());
									logger.info("send register message to {}:{} success",
											s.getAddress().getHostAddress(), s.getPort());
								}
							});
				});
	}

	public TCPConnect connect(int serverType, String ip, int port, Transfer transfer, Parser parser, Handlers handlers, RegisterEvent registerEvent) {
		return connect(serverType, new InetSocketAddress(ip, port), transfer, parser, handlers, registerEvent);
	}

	public TCPConnect connect(int serverType, SocketAddress socketAddress, Transfer transfer, Parser parser, Handlers handlers, RegisterEvent registerEvent) {
		TCPConnect tcpConnection = new TCPConnect(workerGroup,
				socketAddress,
				transfer,
				parser,
				handlers,
				registerEvent);

		tcpConnection.setIdleRunner(c -> {
			ModelProto.ReqHeart heartbeat = ModelProto.ReqHeart.newBuilder()
					.setReqTime(System.currentTimeMillis()).build();

			c.sendMessage(MessageHandel.HEART_REQ, heartbeat, null);
		});

		tcpConnection.connect();

		return tcpConnection;
	}
}
