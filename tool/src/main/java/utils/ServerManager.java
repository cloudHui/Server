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
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
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
import threadtutil.timer.Timer;
import utils.config.ConfigurationManager;
import utils.other.IpUtil;
import utils.other.RandomUtils;

/**
 * 服务连接管理器 - 负责管理与其他服务的TCP连接
 */
public class ServerManager {
	private final static Logger logger = LoggerFactory.getLogger(ServerManager.class);
	private static final int OVER_TIME = 3; // 超时时间(秒)
	private static final int HEARTBEAT_DELAY = 1; // 心跳延迟(秒)
	private static final int JAR_CHECK_DELAY = 3; // Jar检查延迟(秒)
	private static final int JAR_CHECK_INTERVAL = 60; // Jar检查间隔(秒)

	// 连接存储: 服务类型 -> [服务ID -> 连接处理器]
	private final Map<ServerType, Map<Integer, ConnectHandler>> serverMap;
	private final EventLoopGroup workerGroup = new NioEventLoopGroup(1);
	private final Timer timer;
	private GitJarManager gitJar;

	public ServerManager(Timer timer, boolean initJar) {
		serverMap = new HashMap<>();
		this.timer = timer;
		if (initJar) {
			initJar();
		}
	}

	// ==================== Jar管理 ====================

	/**
	 * 初始化Jar代码管理
	 */
	private void initJar() {
		gitJar = new GitJarManager();
		timer.register(JAR_CHECK_DELAY * 1000, JAR_CHECK_INTERVAL * 1000, -1, game -> {
			logger.debug("检查Jar包更新");
			gitJar.checkJarUpdate();
			return false;
		}, this);
	}

	// ==================== 连接管理 ====================

	/**
	 * 添加服务连接
	 */
	public synchronized void addServerClient(ConnectHandler client) {
		ServerInfo connectServer = client.getConnectServer();
		ServerType serverType = ServerType.get(connectServer.getServerType());

		Map<Integer, ConnectHandler> connectMap = serverMap.computeIfAbsent(serverType,
				k -> new ConcurrentHashMap<>());
		connectMap.put(connectServer.getServerId(), client);
	}

	/**
	 * 获取指定服务连接
	 */
	public synchronized ConnectHandler getServerClient(ServerType serverType, int serverId) {
		Map<Integer, ConnectHandler> connectMap = serverMap.get(serverType);
		return connectMap != null ? connectMap.get(serverId) : null;
	}

	/**
	 * 移除服务连接
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
	 * 随机获取指定类型的服务连接
	 */
	public synchronized ConnectHandler getServerClient(ServerType serverType) {
		Map<Integer, ConnectHandler> connectMap = serverMap.get(serverType);
		if (connectMap != null && !connectMap.isEmpty()) {
			List<Integer> serverIds = new ArrayList<>(connectMap.keySet());
			int randomServerId = serverIds.get(RandomUtils.randomRange(serverIds.size()));
			return connectMap.get(randomServerId);
		}
		return null;
	}

	// ==================== 心跳管理 ====================

	/**
	 * 发送心跳消息
	 */
	private void sendHeart(TCPConnect connect) {
		connect.sendMessage(buildHeartMessage(connect.getConnectServer().getServerType()),
				CMsg.HEART, OVER_TIME)
				.whenComplete((message, error) -> {
					if (error != null) {
						logger.error("[发送心跳失败 {}] {}", connect, error.getMessage());
					} else {
						handleHeartbeatResponse(connect, message);
					}
				});
	}

	/**
	 * 处理心跳响应
	 */
	private void handleHeartbeatResponse(TCPConnect connect, Object message) {
		try {
			ModelProto.AckHeart ack = (ModelProto.AckHeart) message;
			long costTime = System.currentTimeMillis() - ack.getReqTime();
			logger.debug("[收到心跳应答 {} 耗时:{}ms]", connect, costTime);

			// 安排下一次心跳
			workerGroup.schedule(() -> sendHeart(connect), HEARTBEAT_DELAY, TimeUnit.SECONDS);
		} catch (Exception ex) {
			logger.error("处理心跳响应异常", ex);
		}
	}

	/**
	 * 构建心跳消息
	 */
	private ModelProto.ReqHeart buildHeartMessage(int serverType) {
		return ModelProto.ReqHeart.newBuilder()
				.setReqTime(System.currentTimeMillis())
				.setServerType(serverType)
				.build();
	}

	// ==================== 服务注册与连接 ====================

	/**
	 * 注册服务(不带处理的 game 专用)
	 */
	public void registerSever(String[] ipPort, Parser parser, ServerType serverType, int serverId, String ipPorts, ServerType localServer) {
		registerSever(ipPort, null, parser, null, serverType, serverId, ipPorts, localServer, null);
	}

	/**
	 * 统一注册服务方法
	 */
	public void registerSever(String[] ipPort, Transfer transfer, Parser parser, Handlers handlers, ServerType connectServer,
							  int serverId, String ipPorts, ServerType localServer, TCPConnect.CallParam callParam) {
		TCPConnect tcpConnect = new TCPConnect(workerGroup,
				new InetSocketAddress(ipPort[0], Integer.parseInt(ipPort[1])),
				transfer, parser, handlers, getActiveHandle(connectServer, localServer), closeHandle);

		// 设置回调参数
		if (callParam != null) {
			tcpConnect.setCallParam(callParam);
		}

		tcpConnect.setLocalServer(new ServerInfo(localServer.getServerType(), serverId, ipPorts));
		tcpConnect.setConnectServer(new ServerInfo(connectServer.getServerType(), ipPort[0] + ":" + ipPort[1]));

		// 如果是连接中心服务器，启用重连机制
		if (connectServer == ServerType.Center) {
			tcpConnect.setConRetry(true);
			tcpConnect.setDiRetry(true);
		}

		tcpConnect.connect();
	}

	/**
	 * 机器人连gate 不需要注册
	 */
	private EventHandle getActiveHandle(ServerType connectServer, ServerType localServer) {
		return connectServer == ServerType.Gate && localServer == ServerType.Robot ? activeHandleRobot : activeHandle;
	}

	/**
	 * 连接到多个服务
	 */
	public void connectToSever(List<ModelProto.ServerInfo> serverInfos, int localServerId, String localIpPort, Transfer transfer,
							   Parser parser, Handlers handlers, ServerType localServer) {
		if (serverInfos == null || serverInfos.isEmpty()) {
			logger.warn("没有需要连接的服务信息");
			return;
		}

		for (ModelProto.ServerInfo serverInfo : serverInfos) {
			connectToSingleServer(serverInfo, localServerId, localIpPort, transfer, parser, handlers, localServer);
		}
	}

	/**
	 * 连接到单个服务
	 */
	private void connectToSingleServer(ModelProto.ServerInfo serverInfo, int localServerId, String localIpPort, Transfer transfer,
									   Parser parser, Handlers handlers, ServerType localServer) {
		String[] ipConfig = serverInfo.getIpConfig().toStringUtf8().split(":");
		ServerType connectServer = ServerType.get(serverInfo.getServerType());

		if (connectServer != null) {
			registerSever(ipConfig, transfer, parser, handlers, connectServer,
					localServerId, localIpPort, localServer, null);
		} else {
			logger.warn("未知的服务类型: {}", serverInfo.getServerType());
		}
	}

	/**
	 * 连接到单个服务
	 */
	public void connectToSingleServer(ModelProto.ServerInfo serverInfo, int localServerId, String localIpPort, Transfer transfer,
									  Parser parser, Handlers handlers, ServerType localServer, TCPConnect.CallParam callParam) {
		String[] ipConfig = serverInfo.getIpConfig().toStringUtf8().split(":");
		ServerType connectServer = ServerType.get(serverInfo.getServerType());

		if (connectServer != null) {
			registerSever(ipConfig, transfer, parser, handlers, connectServer, localServerId, localIpPort, localServer, callParam);
		} else {
			logger.warn("未知的服务类型: {}", serverInfo.getServerType());
		}
	}

	// ==================== 消息构建工具方法 ====================

	/**
	 * 构建服务信息
	 */
	public static ModelProto.ServerInfo buildServerInfo(ConfigurationManager cfgMgr,
														ServerType serverType) {
		return ModelProto.ServerInfo.newBuilder()
				.setServerId(cfgMgr.getInt("id", 0))
				.setServerType(serverType.getServerType())
				.setIpConfig(ByteString.copyFromUtf8(
						IpUtil.getLocalIP() + ":" + cfgMgr.getInt("port", 0))).build();
	}

	/**
	 * 构建注册请求消息
	 */
	private ModelProto.ReqRegister.Builder buildRegisterMessage(ServerInfo localServer) {
		return ModelProto.ReqRegister.newBuilder()
				.setServerInfo(ModelProto.ServerInfo.newBuilder()
						.setServerType(localServer.getServerType())
						.setServerId(localServer.getServerId())
						.setIpConfig(ByteString.copyFromUtf8(localServer.getIpConfig())));
	}

	// ==================== 事件处理器 ====================

	/**
	 * 连接激活事件处理器 - 发送注册消息
	 */
	private final EventHandle activeHandle = channelHandler -> {
		TCPConnect handler = (TCPConnect) channelHandler;
		handler.sendMessage(buildRegisterMessage(handler.getLocalServer()).build(),
				CMsg.REQ_REGISTER, OVER_TIME)
				.whenComplete((message, error) -> {
					if (error != null) {
						logger.error("[注册消息发送失败 {}] {}", handler.getConnectServer(), error.getMessage());
					} else {
						handleRegisterResponse(handler, message);
						executeRegisterCallback(handler);
					}
				});
	};

	/**
	 * 连接激活事件处理器 - 机器人连gate发送注册消息
	 */
	private final EventHandle activeHandleRobot = channelHandler -> {
		executeRegisterCallback((TCPConnect) channelHandler);
	};

	/**
	 * 处理注册响应
	 */
	private void handleRegisterResponse(TCPConnect handler, Object message) {
		try {
			ModelProto.ServerInfo serverInfo = ((ModelProto.AckRegister) message).getServerInfo();
			handler.getConnectServer().setServerId(serverInfo.getServerId());
			addServerClient(handler);
			logger.info("[注册成功:{} {} ]", ServerType.get(handler.getConnectServer().getServerType()), handler.getConnectServer());

			// 开始发送心跳
			workerGroup.schedule(() -> sendHeart(handler), HEARTBEAT_DELAY, TimeUnit.SECONDS);
		} catch (Exception ex) {
			logger.error("处理注册响应异常", ex);
		}
	}

	/**
	 * 执行注册成功回调
	 */
	private void executeRegisterCallback(TCPConnect handler) {
		TCPConnect.CallParam callback = handler.getCallParam();
		if (callback != null) {
			try {
				if (callback.callback != null) {
					callback.callback.handle(callback.messageId, callback.message, handler, callback.parser);
				} else {
					handler.sendMessage(callback.messageId, callback.message);
				}
			} catch (Exception e) {
				logger.error("执行注册成功回调异常", e);
			}
		}
	}

	/**
	 * 连接关闭事件处理器
	 */
	private final EventHandle closeHandle = channelHandler -> {
		TCPConnect connect = (TCPConnect) channelHandler;
		ServerType serverType = ServerType.get(connect.getConnectServer().getServerType());

		if (serverType != null) {
			removeServerClient(serverType, connect.getConnectServer().getServerId());
		}

		logger.error("[连接关闭: {}]", connect.getConnectServer());
	};
}