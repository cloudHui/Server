package utils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.ByteString;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import msg.MessageId;
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

	private final Map<ServerType, Map<Integer, TCPConnect>> serverMap = new ConcurrentHashMap<>();

	private final EventLoopGroup workerGroup = new NioEventLoopGroup();

	/**
	 * 添加服务链接
	 *
	 * @param serverType 服务类型
	 * @param client     链接
	 * @param serverId   服务id
	 */
	public void addServerClient(ServerType serverType, TCPConnect client, int serverId) {
		Map<Integer, TCPConnect> typeMap = serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>());
		TCPConnect tcpConnect = typeMap.get(serverId);
		if (tcpConnect != null) {
			tcpConnect.channelInactive(null);
		}
		typeMap.put(serverId, client);
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
	 * 移除服务链接
	 *
	 * @param serverType 服务类型
	 * @param serverId   服务id
	 */
	public void removeServerClient(ServerType serverType, int serverId) {
		serverMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>()).remove(serverId);
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
			int serverId = list.get(RandomUtils.randomRange(list.size()));
			return serverClient.get(serverId);
		}
		return null;
	}

	/**
	 * 主动链接远程tcp
	 *
	 * @param connect     要链接的服务
	 * @param ip          远程ip
	 * @param port        远程端口
	 * @param transfer    消息转发处理接口
	 * @param parser      消息转化接口
	 * @param handlers    消息处理 handel
	 * @param localServer 本地服务类型
	 * @param localId     本地服务 id
	 * @param localPort   本地服务 IP 端口
	 */
	private void connect(ServerType connect, String ip, int port, Transfer transfer, Parser parser, Handlers handlers, ServerType localServer, int localId, String localPort, int disRetry) {
		connect(connect, new InetSocketAddress(ip, port), transfer, parser, handlers, localServer, localId, localPort, disRetry);
	}

	private void connect(ServerType serverType, SocketAddress socketAddress, Transfer transfer, Parser parser, Handlers handlers, ServerType localServer, int localId, String localPort, int disRetry) {
		connect(
				socketAddress,
				transfer,
				parser,
				handlers,
				(channelHandler -> {
					TCPConnect tcpConnect = (TCPConnect) channelHandler;
					ModelProto.ReqRegister.Builder notice = ModelProto.ReqRegister.newBuilder();
					ModelProto.ServerInfo.Builder server = ModelProto.ServerInfo.newBuilder();
					server.setServerType(localServer.getServerType());
					server.setServerId(localId);
					server.setIpConfig(ByteString.copyFromUtf8(localPort));
					notice.setServerInfo(server.build());

					tcpConnect.sendMessage(MessageId.REQ_REGISTER, notice.build(), null, 3)
							.whenComplete((r, e) -> {
								InetSocketAddress s = (InetSocketAddress) socketAddress;
								if (null != e) {
									logger.error("ERROR! failed for send register message to {}:{}",
											s.getAddress().getHostAddress(), s.getPort(), e);
								} else {
									ModelProto.ServerInfo serverInfo = ((ModelProto.AckRegister) r).getServerInfo();
									tcpConnect.setServerId(serverInfo.getServerId());

									try {
										addServerClient(serverType, tcpConnect, serverInfo.getServerId());
									} catch (Exception ex) {
										ex.printStackTrace();
									}
									logger.info("send register message to {}:{} success",
											s.getAddress().getHostAddress(), s.getPort());
								}
							});
				}), localServer, disRetry);
	}

	private void connect(SocketAddress socketAddress, Transfer transfer, Parser parser, Handlers handlers, RegisterEvent registerEvent, ServerType localServer, int disRetry) {
		TCPConnect tcpConnection = new TCPConnect(workerGroup,
				socketAddress,
				transfer,
				parser,
				handlers,
				registerEvent);

		//连接后 触发 读,写空闲事件后的处理 发送心跳
		tcpConnection.setIdleRunner(handler -> {
			ModelProto.ReqHeart heartbeat = ModelProto.ReqHeart.newBuilder()
					.setReqTime(System.currentTimeMillis())
					.setServerType(localServer.getServerType()).build();
			handler.sendMessage(MessageId.HEART, heartbeat, null);
		});

		tcpConnection.connect(disRetry);
	}

	/**
	 * 链接
	 */
	public TCPConnect connect(SocketAddress socketAddress, Transfer transfer, Parser parser, Handlers handlers, int disRetry) {
		TCPConnect tcpConnection = new TCPConnect(workerGroup,
				socketAddress,
				transfer,
				parser,
				handlers,
				null);
		return tcpConnection.connect(disRetry);
	}


	/**
	 * 注册服务
	 */
	public void registerSever(String[] ipPort, Transfer transfer, Parser parser, Handlers handlers, ServerType serverType,
							  int serverId, String ipPorts, ServerType connectServer, int disRetry) {
		connect(connectServer, ipPort[0], Integer.parseInt(ipPort[1]), transfer, parser,
				handlers, serverType, serverId, ipPorts, disRetry);
	}

	/**
	 * 链接
	 */
	public TCPConnect connectSever(String[] ipPort, Transfer transfer, Parser parser, Handlers handlers, int disRetry) {
		return connect(new InetSocketAddress(ipPort[0], Integer.parseInt(ipPort[1])), transfer, parser,
				handlers, disRetry);
	}

	public boolean connectToSever(List<ModelProto.ServerInfo> serverInfos, int serverId, String ipPort, Transfer transfer, Parser parser, Handlers handlers) {
		if (serverInfos == null || serverInfos.isEmpty()) {
			return true;
		}
		String[] ipConfig;
		ServerType serverType;
		for (ModelProto.ServerInfo serverInfo : serverInfos) {
			ipConfig = serverInfo.getIpConfig().toStringUtf8().split(":");
			serverType = ServerType.get(serverInfo.getServerType());
			if (serverType != null) {
				registerSever(ipConfig, transfer, parser, handlers, ServerType.Gate, serverId, ipPort, serverType, 0);
				logger.error("[register server:{} info:{}]", serverType, serverInfo.toString());
			}
		}
		return true;
	}
}
