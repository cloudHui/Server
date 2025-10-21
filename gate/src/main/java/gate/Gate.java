package gate;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import gate.client.ClientProto;
import gate.client.GateTcpClient;
import gate.client.handle.back.BackHandleManager;
import gate.connect.ConnectProcessor;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.connect.TCPConnect;
import net.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import threadtutil.thread.ExecutorPool;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.ServerManager;
import utils.config.ConfigurationManager;
import utils.manager.HandleManager;
import utils.other.IpUtil;

/**
 * 网关服务器主类
 * 负责启动和管理网关服务
 */
public class Gate {
	private static final Logger logger = LoggerFactory.getLogger(Gate.class);
	private static final Gate instance = new Gate();

	private final ExecutorPool executorPool;
	private final Timer timer;

	private int port;
	private int serverId;
	private String innerIp;
	private String center;

	private ServerManager serverManager;

	private Gate() {
		executorPool = new ExecutorPool("Gate");
		timer = new Timer().setRunners(executorPool);
	}

	public static Gate getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		try {
			logger.info("正在启动网关服务器...");
			instance.start();
		} catch (Exception e) {
			logger.error("网关服务器启动失败", e);
			System.exit(1);
		}
	}

	// Getter和Setter方法
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getServerId() {
		return serverId;
	}

	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

	public String getInnerIp() {
		return innerIp;
	}

	public void setInnerIp(String innerIp) {
		this.innerIp = innerIp;
	}

	public void setCenter(String center) {
		this.center = center;
	}

	public ServerManager getServerManager() {
		return serverManager;
	}

	public void execute(Runnable task) {
		executorPool.execute(task);
	}


	public <T> void registerTimer(long delay, long interval, int count, Runner<T> runner, T param) {
		timer.register(delay, interval, count, runner, param);
		logger.debug("注册定时器, delay: {}, interval: {}, count: {}", delay, interval, count);
	}

	/**
	 * 启动网关服务器
	 */
	private void start() {
		try {
			// 加载配置
			loadConfiguration();

			// 初始化组件
			initializeComponents();

			// 启动网络服务
			startNetworkServices();

			// 注册到中心服务器
			registerToCenter();

			logger.info("网关服务器启动成功! 服务器ID: {}, 地址: {}:{}", serverId, innerIp, port);

		} catch (Exception e) {
			logger.error("网关服务器启动过程中发生错误", e);
			throw new RuntimeException("网关服务器启动失败", e);
		}
	}

	/**
	 * 加载配置文件
	 */
	private void loadConfiguration() {
		ConfigurationManager config = ConfigurationManager.getInstance();

		setPort(config.getInt("port", 0));
		int wsPort = config.getInt("wsGate", 0);
		setServerId(config.getInt("id", 0));
		setCenter(config.getProperty("center"));
		setInnerIp(IpUtil.getLocalIP());

		logger.info("配置加载完成 - 端口: {}, WebSocket端口: {}, 服务器ID: {}, 中心服务器: {}",
				port, wsPort, serverId, center);
	}

	/**
	 * 初始化组件
	 */
	private void initializeComponents() {
		serverManager = new ServerManager(timer, ConfigurationManager.getInstance().getInt("plant", 0) != 0);
		ConnectProcessor.init();
		ClientProto.init();
		HandleManager.init(ConnectProcessor.class);
		BackHandleManager.init();
		logger.info("服务器管理器初始化完成");
	}

	/**
	 * 启动网络服务
	 */
	private void startNetworkServices() {
		List<SocketAddress> addresses = new ArrayList<>();

		// 启动TCP服务
		addresses.add(new InetSocketAddress(innerIp, port));
		new ServerService(90, GateTcpClient.class).start(addresses);
		logger.info("TCP服务启动成功, 端口: {}", port);

		// 启动WebSocket服务
		addresses.clear();
		int wsPort = ConfigurationManager.getInstance().getInt("wsGate", 0);
		addresses.add(new InetSocketAddress(innerIp, wsPort));
		new GateWsService().start(addresses);
		logger.info("WebSocket服务启动成功, 端口: {}", wsPort);
	}

	/**
	 * 注册到中心服务器
	 */
	private void registerToCenter() {
		try {
			String[] centerAddress = center.split(":");
			if (centerAddress.length != 2) {
				throw new IllegalArgumentException("中心服务器地址格式错误: " + center);
			}

			String serverAddress = innerIp + ":" + port;
			ModelProto.ReqServerInfo serverInfo = buildServerInfoRequest();

			serverManager.registerSever(centerAddress, null, ConnectProcessor.PARSER,
					ConnectProcessor.HANDLERS, ServerType.Center, serverId,
					serverAddress, ServerType.Gate,
					new TCPConnect.CallParam(CMsg.REQ_SERVER, serverInfo,
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
				.addServerType(ServerType.Game.getServerType())
				.addServerType(ServerType.Hall.getServerType())
				.addServerType(ServerType.Room.getServerType())
				.build();
	}
}