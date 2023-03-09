package utils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

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

	private Map<ServerType, Map<Integer, TCPConnect>> serverMap = new ConcurrentHashMap<>();

	private final EventLoopGroup workerGroup = new NioEventLoopGroup();

	/**
	 * 添加服务链接
	 *
	 * @param serverType 服务类型
	 * @param client     链接
	 * @param serverId   服务id
	 */
	private void addServerClient(ServerType serverType, TCPConnect client, int serverId) {
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

	/**
	 * 主动链接远程tcp
	 *
	 * @param ip          远程ip
	 * @param port        远程端口
	 * @param transfer    消息转发处理接口
	 * @param parser      消息转化接口
	 * @param handlers    消息处理 handel
	 * @param localServer 本地服务类型
	 * @param localId     本地服务 id
	 * @param localPort   本地服务 IP 端口
	 */
	public void connect(String ip, int port, Transfer transfer, Parser parser, Handlers handlers, ServerType localServer, int localId, String localPort) {
		connect(new InetSocketAddress(ip, port), transfer, parser, handlers, localServer, localId, localPort);
	}

	public void connect(SocketAddress socketAddress, Transfer transfer, Parser parser, Handlers handlers, ServerType localServer, int localId, String localPort) {
		connect(
				socketAddress,
				transfer,
				parser,
				handlers,
				(RegisterEvent<TCPConnect>) tcpConnect -> {
					ModelProto.ReqRegister.Builder notice = ModelProto.ReqRegister.newBuilder();
					ModelProto.ServerInfo.Builder server = ModelProto.ServerInfo.newBuilder();
					server.setServerType(localServer.getServerType());
					server.setServerId(localId);
					server.setIpConfig(localPort);
					notice.setServerInfo(server.build());


					tcpConnect.sendMessage(MessageHandel.REGISTER, notice.build(), null, 10)
							.whenComplete((BiConsumer<ModelProto.AckRegister, Exception>) (r, e) -> {
								InetSocketAddress s = (InetSocketAddress) socketAddress;
								if (null != e) {
									logger.error("ERROR! failed for send register message to {}:{}",
											s.getAddress().getHostAddress(), s.getPort(), e);
								} else {
									ModelProto.ServerInfo serverInfo = r.getServerInfo();
									tcpConnect.setServerId(serverInfo.getServerId());

									addServerClient(ServerType.get(serverInfo.getServerType()), tcpConnect, serverInfo.getServerId());
									logger.info("send register message to {}:{} success",
											s.getAddress().getHostAddress(), s.getPort());
								}
							});
				});
	}

	public void connect(SocketAddress socketAddress, Transfer transfer, Parser parser, Handlers handlers, RegisterEvent registerEvent) {
		TCPConnect tcpConnection = new TCPConnect(workerGroup,
				socketAddress,
				transfer,
				parser,
				handlers,
				registerEvent);

		//连接后的操作
		tcpConnection.setIdleRunner(handler -> {
			ModelProto.ReqHeart heartbeat = ModelProto.ReqHeart.newBuilder()
					.setReqTime(System.currentTimeMillis()).build();
			handler.sendMessage(MessageHandel.HEART_REQ, heartbeat, null);
		});

		tcpConnection.connect();

	}
}
