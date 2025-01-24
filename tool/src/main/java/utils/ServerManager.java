package utils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import io.netty.channel.EventLoopGroup;
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

	private final Map<ServerType, Map<Integer, TCPConnect>> serverMap;

	private final EventLoopGroup workerGroup;

	public ServerManager(EventLoopGroup workerGroup) {
		this.workerGroup = workerGroup;
		serverMap = new ConcurrentHashMap<>();
	}

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
	private void connect(ServerType connect, String ip, int port, Transfer transfer, Parser parser, Handlers handlers, ServerType localServer, int localId, String localPort) {
		connect(connect, new InetSocketAddress(ip, port), transfer, parser, handlers, localServer, localId, localPort);
	}

	private void connect(ServerType serverType, SocketAddress socketAddress, Transfer transfer, Parser parser, Handlers handlers, ServerType localServer, int localId, String localPort) {
		connect(socketAddress,
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

					tcpConnect.sendMessage(MessageId.REQ_REGISTER, notice.build(), 3)
							.whenComplete((r, e) -> {
								InetSocketAddress s = (InetSocketAddress) socketAddress;
								if (null != e) {
									logger.error("ERROR! failed for send register message to {}:{}", s.getAddress().getHostAddress(), s.getPort(), e);
								} else {
									try {
										ModelProto.ServerInfo serverInfo = ((ModelProto.AckRegister) r).getServerInfo();
										tcpConnect.setServerId(serverInfo.getServerId());
										addServerClient(serverType, tcpConnect, serverInfo.getServerId());
										logger.info("receive register message to {}:{} success", s.getAddress().getHostAddress(), s.getPort());
									} catch (Exception ex) {
										ex.printStackTrace();
									}
								}
							});
				}), localServer, serverType);
	}

	private void connect(SocketAddress address, Transfer transfer, Parser parser, Handlers handlers, RegisterEvent registerEvent, ServerType localServer, ServerType connect) {
		TCPConnect tcpConnect = new TCPConnect(workerGroup,
				address,
				transfer,
				parser,
				handlers,
				registerEvent);

		//链接关闭事件 移除链接关闭链接
		tcpConnect.setCloseEvent((handler) -> removeServerClient(connect, (int) ((TCPConnect) handler).getServerId()));
		//调度链接 断连重试
		workerGroup.scheduleWithFixedDelay(() -> {
			TCPConnect handler = getServerClient(connect);
			if (handler != null) {
				return;
			}
			tcpConnect.connect();
			//调度发送心跳
			workerGroup.scheduleWithFixedDelay(() -> sendHearReq(localServer.getServerType(), 0, address, connect), 3L, 1L, TimeUnit.SECONDS);
		}, 0L, 1L, TimeUnit.SECONDS);

	}

	/**
	 * 组织心跳消息
	 */
	private ModelProto.ReqHeart manageHeart(int serverType, int retryTime) {
		return ModelProto.ReqHeart.newBuilder()
				.setReqTime(System.currentTimeMillis())
				.setRetryTime(retryTime)
				.setServerType(serverType).build();
	}

	/**
	 * 发送心跳请求并回调处理
	 */
	private void sendHearReq(int localServer, int retryTime, SocketAddress address, ServerType connect) {
		//调度延迟发送心跳
		TCPConnect handler = getServerClient(connect);
		if (handler == null) {
			return;
		}
		handler.sendMessage(MessageId.HEART, manageHeart(localServer, retryTime), 3).whenComplete((message, e) -> {
			if (null != e) {
				logger.error("ERROR! failed for send HEART  message {}:{} retryTime:{} {}", ((InetSocketAddress) address).getHostName(),
						((InetSocketAddress) address).getPort(), retryTime, e.getMessage());
			} else {
				try {
					ModelProto.AckHeart ack = ((ModelProto.AckHeart) message);
					long cost = System.currentTimeMillis() - ack.getReqTime();
					logger.info("receive connect:{}  HEART_ACK message {}:{} cost:{}ms retryTime:{} success", connect, ((InetSocketAddress) address).getHostName(),
							((InetSocketAddress) address).getPort(), cost, ack.getRetryTime());
					return;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			if (retryTime + 1 <= 3) {
				sendHearReq(localServer, retryTime + 1, address, connect);
				return;
			}
			handler.channelInactive(null);
			logger.error("close connect {}:{} ", ((InetSocketAddress) address).getHostName(), ((InetSocketAddress) address).getPort());
		});
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
	 *
	 * @param serverType  要链接的服务类型
	 * @param localServer 本地服务类型
	 */
	public void registerSever(String[] ipPort, Transfer transfer, Parser parser, Handlers handlers, ServerType serverType, int serverId, String ipPorts, ServerType localServer) {
		connect(serverType, ipPort[0], Integer.parseInt(ipPort[1]), transfer, parser, handlers, localServer, serverId, ipPorts);
	}

	/**
	 * 链接
	 */
	public TCPConnect connectSever(String[] ipPort, Transfer transfer, Parser parser, Handlers handlers, int disRetry) {
		return connect(new InetSocketAddress(ipPort[0], Integer.parseInt(ipPort[1])), transfer, parser, handlers, disRetry);
	}


	public boolean connectToSever(List<ModelProto.ServerInfo> serverInfos, int localServerId, String localIpPort, Transfer transfer, Parser parser, Handlers handlers, ServerType localServer) {
		if (serverInfos == null || serverInfos.isEmpty()) {
			return true;
		}
		String[] ipConfig;
		ServerType connectServer;
		for (ModelProto.ServerInfo serverInfo : serverInfos) {
			ipConfig = serverInfo.getIpConfig().toStringUtf8().split(":");
			connectServer = ServerType.get(serverInfo.getServerType());
			if (connectServer != null) {
				registerSever(ipConfig, transfer, parser, handlers, connectServer, localServerId, localIpPort, localServer);
				logger.error("[registerSever server:{} info:{}]", connectServer, serverInfo.toString());
			}
		}
		return true;
	}
}
