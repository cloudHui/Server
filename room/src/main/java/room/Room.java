package room;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.connect.TCPConnect;
import net.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import room.client.ClientProto;
import room.client.RoomClient;
import room.connect.ConnectProcessor;
import room.manager.RoomManager;
import threadtutil.thread.ExecutorPool;
import threadtutil.timer.Timer;
import utils.ServerClientManager;
import utils.ServerManager;
import utils.config.ConfigurationManager;
import utils.config.ServerConfiguration;

/**
 * 房间服务器主类
 * 负责启动和管理房间服务
 */
public class Room {
	private static final Logger logger = LoggerFactory.getLogger(Room.class);
	private static final Room instance = new Room();

	private final ExecutorPool executorPool;
	private final Timer timer;
	private final ServerClientManager serverClientManager = new ServerClientManager();

	private int serverId;
	private String center;
	private ModelProto.ServerInfo.Builder serverInfo;
	private ServerManager serverManager;

	private Room() {
		executorPool = new ExecutorPool("Room");
		timer = new Timer().setRunners(executorPool);
	}

	public static Room getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		try {
			logger.info("正在启动房间服务器...");
			instance.start();
			logger.info("房间服务器启动成功");
		} catch (Exception e) {
			logger.error("房间服务器启动失败", e);
			System.exit(1);
		}
	}

	// Getter方法
	public int getServerId() {
		return serverId;
	}

	public ServerManager getServerManager() {
		return serverManager;
	}

	public ModelProto.ServerInfo.Builder getServerInfo() {
		return serverInfo;
	}

	public ServerClientManager getServerClientManager() {
		return serverClientManager;
	}

	public void execute(Runnable task) {
		executorPool.execute(task);
	}

	/**
	 * 启动房间服务器
	 */
	private void start() {
		try {
			// 1. 加载配置
			loadConfiguration();

			// 2. 初始化组件
			initializeComponents();

			// 3. 启动网络服务
			startNetworkService();

			// 4. 注册到中心服务器
			registerToCenter();

			// 5. 初始化房间管理器
			initializeRoomManager();

			logger.info("房间服务器启动完成! 服务器ID: {}, 地址: {}", serverId, serverInfo.getIpConfig().toStringUtf8());

		} catch (Exception e) {
			logger.error("房间服务器启动过程中发生错误", e);
			throw new RuntimeException("房间服务器启动失败", e);
		}
	}

	/**
	 * 加载配置
	 */
	private void loadConfiguration() {
		ConfigurationManager config = ConfigurationManager.getInstance();

		// 获取服务器配置
		ServerConfiguration serverConfig = config.getServers().get("room");
		if (serverConfig == null || !serverConfig.hasHostString()) {
			throw new RuntimeException("找不到房间服务器配置");
		}

		serverId = config.getInt("id", 0);
		center = config.getProperty("center");
		serverInfo = ServerManager.buildServerInfo(config, ServerType.Room);

		logger.info("配置加载完成 - 服务器ID: {}, 中心服务器: {}", serverId, center);
	}

	/**
	 * 初始化组件
	 */
	private void initializeComponents() {
		serverManager = new ServerManager(timer, ConfigurationManager.getInstance().getInt("plant", 0) != 0);
		logger.info("服务器管理器初始化完成");
	}

	/**
	 * 启动网络服务
	 */
	private void startNetworkService() {
		String[] addressParts = serverInfo.getIpConfig().toStringUtf8().split(":");
		if (addressParts.length != 2) {
			throw new RuntimeException("服务器地址格式错误: " + serverInfo.getIpConfig().toStringUtf8());
		}

		String host = addressParts[0];
		int port = Integer.parseInt(addressParts[1]);

		List<SocketAddress> addresses = new ArrayList<>();
		addresses.add(new InetSocketAddress(host, port));

		new ServerService(0, RoomClient.class).start(addresses);
		logger.info("网络服务启动成功, 地址: {}:{}", host, port);
	}

	/**
	 * 注册到中心服务器
	 */
	private void registerToCenter() {
		try {
			ClientProto.init();
			ConnectProcessor.init();

			String[] centerAddress = center.split(":");
			if (centerAddress.length != 2) {
				throw new IllegalArgumentException("中心服务器地址格式错误: " + center);
			}

			ModelProto.ReqServerInfo serverInfoRequest = buildServerInfoRequest();

			serverManager.registerSever(centerAddress, ConnectProcessor.TRANSFER,
					ConnectProcessor.PARSER, ConnectProcessor.HANDLERS,
					ServerType.Center, serverId, serverInfo.getIpConfig().toStringUtf8(),
					ServerType.Room, new TCPConnect.CallParam(CMsg.REQ_SERVER, serverInfoRequest));

			logger.info("已向中心服务器注册, 地址: {}:{}", centerAddress[0], centerAddress[1]);

		} catch (Exception e) {
			logger.error("向中心服务器注册失败", e);
			throw new RuntimeException("中心服务器注册失败", e);
		}
	}

	/**
	 * 构建服务器信息请求
	 */
	private ModelProto.ReqServerInfo buildServerInfoRequest() {
		return ModelProto.ReqServerInfo.newBuilder()
				.addServerType(ServerType.Game.getServerType())
				.build();
	}

	/**
	 * 初始化房间管理器
	 */
	private void initializeRoomManager() {
		RoomManager.getInstance().init();
		logger.info("房间管理器初始化完成");
	}
}