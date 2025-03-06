package utils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import msg.MessageId;
import msg.ServerType;
import net.client.event.EventHandle;
import net.connect.ServerInfo;
import net.connect.TCPConnect;
import net.connect.handle.ConnectHandler;
import net.handler.Handlers;
import net.message.Parser;
import net.message.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.config.ConfigurationManager;
import utils.utils.IpUtil;
import utils.utils.RandomUtils;

/**
 * 服务链接管理
 */
public class ServerManager {
	private final static Logger logger = LoggerFactory.getLogger(ServerManager.class);

	/**
	 * 链接类型 链接服务id  链接
	 */
	private final Map<ServerType, Map<Integer, ConnectHandler>> serverMap;

	private final EventLoopGroup workerGroup = new NioEventLoopGroup(1);

	private final static int OVER_TIME = 3;//超时时间S

	public ServerManager() {
		serverMap = new HashMap<>();
	}

	/**
	 * 添加服务链接
	 *
	 * @param client 链接
	 */
	public synchronized void addServerClient(ConnectHandler client) {
		ServerInfo connectServer = client.getConnectServer();
		ServerType serverType = ServerType.get(connectServer.getServerType());
		Map<Integer, ConnectHandler> connectMap = serverMap.get(serverType);
		if (connectMap == null) {
			connectMap = new ConcurrentHashMap<>();
			serverMap.put(serverType, connectMap);
		}
		connectMap.put(connectServer.getServerId(), client);
	}

	/**
	 * 获取指定服务id 类型的服务链接
	 *
	 * @param serverType 服务类型
	 * @param serverId   链接id
	 */
	public synchronized ConnectHandler getServerClient(ServerType serverType, int serverId) {
		Map<Integer, ConnectHandler> connectMap = serverMap.get(serverType);
		if (connectMap != null) {
			return connectMap.get(serverId);
		}
		return null;
	}

	/**
	 * 移除服务链接
	 *
	 * @param serverType 服务类型
	 * @param serverId   服务id
	 */
	public synchronized void removeServerClient(ServerType serverType, int serverId) {
		Map<Integer, ConnectHandler> connectMap = serverMap.get(serverType);
		if (connectMap != null) {
			connectMap.remove(serverId);
			if (connectMap.isEmpty()) {
				serverMap.remove(serverType);
			}
		}
	}

	/**
	 * 获取随机类型服务链接
	 *
	 * @param serverType 服务类型
	 */
	public synchronized ConnectHandler getServerClient(ServerType serverType) {
		Map<Integer, ConnectHandler> connectMap = serverMap.get(serverType);
		if (connectMap != null && !connectMap.isEmpty()) {
			List<Integer> list = new ArrayList<>(connectMap.keySet());
			int serverId = list.get(RandomUtils.randomRange(list.size()));
			return connectMap.get(serverId);
		}
		return null;
	}

	/**
	 * 发送心跳
	 */
	private void sendHeart(TCPConnect connect) {
		connect.sendMessage(MessageId.HEART, manageHeart(connect.getConnectServer().getServerType()), OVER_TIME)
				.whenComplete((message, e) -> {
					if (null != e) {
						logger.error("[ERROR! failed for send HEART  connect {}] {}", connect, e.getMessage());
					} else {
						try {
							ModelProto.AckHeart ack = ((ModelProto.AckHeart) message);
							long cost = System.currentTimeMillis() - ack.getReqTime();
							logger.debug("[receive connect:{} HEART_ACK cost:{}ms success]", connect, cost);
							workerGroup.schedule(() -> sendHeart(connect), 1, TimeUnit.SECONDS);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				});
	}


	/**
	 * 组织心跳消息
	 */
	private ModelProto.ReqHeart manageHeart(int serverType) {
		return ModelProto.ReqHeart.newBuilder()
				.setReqTime(System.currentTimeMillis())
				.setServerType(serverType).build();
	}

	/**
	 * 注册服务
	 *
	 * @param serverType  要链接的服务类型
	 * @param localServer 本地服务类型
	 */
	public void registerSever(String[] ipPort, Transfer transfer, Parser parser, Handlers handlers, ServerType serverType, int serverId, String ipPorts, ServerType localServer) {
		TCPConnect tConnect = new TCPConnect(workerGroup,
				new InetSocketAddress(ipPort[0], Integer.parseInt(ipPort[1])),
				transfer,
				parser,
				handlers,
				activeHandle,
				closeHandle);
		tConnect.setLocalServer(new ServerInfo(localServer.getServerType(), serverId, ipPorts));
		tConnect.setConnectServer(new ServerInfo(serverType.getServerType(), 0, (ipPort[0] + ":" + ipPort[1])));
		//助弱要连接的是注册服务 需要设置重连重试和断链重试
		if (serverType == ServerType.Center) {
			tConnect.setConRetry(true);
			tConnect.setDiRetry(true);
		}
		tConnect.connect();
	}

	/**
	 * 注册链接到服务
	 *
	 * @param serverInfos   需要注册连接的服务信息集合
	 * @param localServerId 本地服务id
	 * @param localIpPort   本地服务端口
	 * @param transfer      转发器
	 * @param parser        转码器
	 * @param handlers      处理器
	 * @param localServer   本地服务类型
	 */
	public void connectToSever(List<ModelProto.ServerInfo> serverInfos, int localServerId, String localIpPort, Transfer transfer, Parser parser, Handlers handlers, ServerType localServer) {
		if (serverInfos == null || serverInfos.isEmpty()) {
			return;
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
	}

	/**
	 * 组织服务信息
	 */
	public static ModelProto.ServerInfo.Builder manageServerInfo(ConfigurationManager cfgMgr, ServerType serverType) {
		ModelProto.ServerInfo.Builder serverInfo = ModelProto.ServerInfo.newBuilder();
		serverInfo.setServerId(cfgMgr.getInt("id", 0));
		serverInfo.setServerType(serverType.getServerType());
		serverInfo.setIpConfig(ByteString.copyFromUtf8(IpUtil.getLocalIP() + ":" + cfgMgr.getInt("port", 0)));
		return serverInfo;
	}

	/**
	 * 组织注册消息
	 *
	 * @param localServer 本地服务
	 */
	private ModelProto.ReqRegister.Builder manageReqRegister(ServerInfo localServer) {
		return ModelProto.ReqRegister.newBuilder().setServerInfo(ModelProto.ServerInfo.newBuilder()
				.setServerType(localServer.getServerType())
				.setServerId(localServer.getServerId())
				.setIpConfig(ByteString.copyFromUtf8(localServer.getIpConfig())));
	}


	/**
	 * 服务链接成功
	 * 注册服务消息和注册成功 固定延时间隔发送心跳
	 */
	private final EventHandle activeHandle = channelHandler -> {
		TCPConnect handler = (TCPConnect) channelHandler;
		handler.sendMessage(MessageId.REQ_REGISTER, manageReqRegister(handler.getLocalServer()).build(), OVER_TIME)
				.whenComplete((message, e) -> {
					if (null != e) {
						logger.error("[ERROR! failed send register message to {} {}]", handler.getConnectServer(), e.getMessage());
					} else {
						try {
							ModelProto.ServerInfo serverInfo = ((ModelProto.AckRegister) message).getServerInfo();
							handler.getConnectServer().setServerId(serverInfo.getServerId());
							addServerClient(handler);
							logger.info("[receive register message to {} success]", handler.getConnectServer());
							workerGroup.schedule(() -> sendHeart(handler), 1, TimeUnit.SECONDS);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				});
	};

	/**
	 * 结束关闭处理断开链接
	 */
	private final EventHandle closeHandle = channelHandler -> {
		TCPConnect connect = (TCPConnect) channelHandler;
		ServerType serverType = ServerType.get(connect.getConnectServer().getServerType());
		if (serverType != null) {
			removeServerClient(serverType, connect.getConnectServer().getServerId());
		}
		logger.error("[closeHandle:{}]", connect.getConnectServer());
	};
}
