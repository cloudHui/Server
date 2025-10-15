package hall;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import hall.client.ClientProto;
import hall.client.HallClient;
import hall.connect.ConnectProcessor;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.connect.TCPConnect;
import net.service.ServerService;
import proto.ModelProto;
import threadtutil.thread.ExecutorPool;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.ServerClientManager;
import utils.ServerManager;
import utils.config.ConfigurationManager;
import utils.manager.HandleManager;
import utils.other.IpUtil;

/**
 * 大厅服务器主类
 * 负责用户登录、会话管理和房间信息协调
 */
public class Hall {
	private static final Logger logger = LoggerFactory.getLogger(Hall.class);
	private static final Hall instance = new Hall();

	private final ExecutorPool executorPool;
	private final Timer timer;
	public final ServerClientManager serverClientManager = new ServerClientManager();

	private int serverId;
	private String center;
	private String innerIp;
	private int port;
	private ModelProto.ServerInfo.Builder serverInfo;
	private ServerManager serverManager;

	private Hall() {
		executorPool = new ExecutorPool("Hall");
		timer = new Timer().setRunners(executorPool);
	}

	public static Hall getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		try {
			logger.info("正在启动大厅服务器...");
			instance.start();
			logger.info("大厅服务器启动成功");
		} catch (Exception e) {
			logger.error("大厅服务器启动失败", e);
			System.exit(1);
		}
	}

	// Getter和Setter方法
	public int getServerId() {
		return serverId;
	}

	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

	public void setCenter(String center) {
		this.center = center;
	}

	public String getInnerIp() {
		return innerIp;
	}

	public void setInnerIp(String innerIp) {
		this.innerIp = innerIp;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public ServerManager getServerManager() {
		return serverManager;
	}

	public ModelProto.ServerInfo.Builder getServerInfo() {
		return serverInfo;
	}

	public void execute(Runnable task) {
		executorPool.execute(task);
	}

	public <T> void registerTimer(long delay, long interval, int count, Runner<T> runner, T param) {
		timer.register(delay, interval, count, runner, param);
		logger.debug("注册定时器, delay: {}, interval: {}, count: {}", delay, interval, count);
	}

	/**
	 * 启动大厅服务器
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

			logger.info("大厅服务器启动完成! 服务器ID: {}, 地址: {}:{}", serverId, innerIp, port);

		} catch (Exception e) {
			logger.error("大厅服务器启动过程中发生错误", e);
			throw new RuntimeException("大厅服务器启动失败", e);
		}
	}

	/**
	 * 加载配置
	 */
	private void loadConfiguration() {
		ConfigurationManager config = ConfigurationManager.getInstance();

		serverInfo = ServerManager.buildServerInfo(config, ServerType.Hall);
		setServerId(config.getInt("id", 0));
		setInnerIp(IpUtil.getLocalIP());
		setPort(config.getInt("port", 0));
		setCenter(config.getProperty("center"));

		logger.info("配置加载完成 - 服务器ID: {}, 端口: {}, 中心服务器: {}", serverId, port, center);
	}

	/**
	 * 初始化组件
	 */
	private void initializeComponents() {
		serverManager = new ServerManager(timer,
				ConfigurationManager.getInstance().getInt("plant", 0) != 0);
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

		new ServerService(0, HallClient.class).start(addresses);
		logger.info("网络服务启动成功, 地址: {}:{}", host, port);
	}

	/**
	 * 注册到中心服务器
	 */
	private void registerToCenter() {
		try {
			ClientProto.init();
			ConnectProcessor.init();
			HandleManager.init(ConnectProcessor.class);

			String[] centerAddress = center.split(":");
			if (centerAddress.length != 2) {
				throw new IllegalArgumentException("中心服务器地址格式错误: " + center);
			}

			ModelProto.ReqServerInfo serverInfoRequest = buildServerInfoRequest();

			serverManager.registerSever(centerAddress, ConnectProcessor.TRANSFER,
					ConnectProcessor.PARSER, ConnectProcessor.HANDLERS,
					ServerType.Center, serverId, serverInfo.getIpConfig().toStringUtf8(),
					ServerType.Hall, new TCPConnect.CallParam(CMsg.REQ_SERVER, serverInfoRequest,
							HandleManager::sendMsg, ConnectProcessor.PARSER));

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
				.addServerType(ServerType.Room.getServerType())
				.build();
	}
}