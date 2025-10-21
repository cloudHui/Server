package game;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import game.client.ClientProto;
import game.client.GameClient;
import game.manager.TableManager;
import msg.registor.HandleTypeRegister;
import msg.registor.enums.ServerType;
import net.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.ServerClientManager;
import utils.ServerManager;
import utils.config.ConfigurationManager;

/**
 * 游戏服务器主类
 * 负责游戏逻辑处理、桌子管理和玩家会话
 */
public class Game {
	private static final Logger logger = LoggerFactory.getLogger(Game.class);
	private static final Game instance = new Game();

	private final ServerClientManager serverClientManager = new ServerClientManager();
	private ExecutorPool executorPool;
	private Timer timer;
	private int serverId;
	private String center;
	private ModelProto.ServerInfo serverInfo;
	private ServerManager serverManager;
	private TableManager tableManager;

	private Game() {
		// 私有构造函数,单例模式
	}

	public static Game getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		try {
			logger.info("正在启动游戏服务器...");
			instance.start();
			logger.info("游戏服务器启动成功");
		} catch (Exception e) {
			logger.error("游戏服务器启动失败", e);
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

	public ServerManager getServerManager() {
		return serverManager;
	}

	public ServerClientManager getServerClientManager() {
		return serverClientManager;
	}

	public TableManager getTableManager() {
		return tableManager;
	}

	public ModelProto.ServerInfo getServerInfo() {
		return serverInfo;
	}

	/**
	 * 注册定时器
	 */
	public <T> void registerTimer(long delay, long interval, int count, Runner<T> runner, T param) {
		timer.register(delay, interval, count, runner, param);
		logger.debug("注册定时器, delay: {}, interval: {}, count: {}", delay, interval, count);
	}

	/**
	 * 注册串行定时器
	 */
	public <T> void registerSerialTimer(int groupId, long delay, long interval, int count, Runner<T> runner, T param) {
		timer.registerSerial(groupId, delay, interval, count, runner, param);
		logger.debug("注册串行定时器, groupId: {}, delay: {}, interval: {}", groupId, delay, interval);
	}

	/**
	 * 直接提交任务到线程池
	 */
	public void execute(Runnable task) {
		executorPool.execute(task);
		logger.debug("提交任务到线程池");
	}

	/**
	 * 按顺序有序处理任务
	 */
	public void serialExecute(Task task) {
		executorPool.serialExecute(task);
		logger.debug("提交串行任务");
	}

	public int getPooSize(){
		return executorPool.size();
	}

	/**
	 * 启动游戏服务器
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

			// 5. 初始化游戏管理器
			initializeGameManagers();

			logger.info("游戏服务器启动完成! 服务器ID: {}, 地址: {}",
					serverId, serverInfo.getIpConfig().toStringUtf8());

		} catch (Exception e) {
			logger.error("游戏服务器启动过程中发生错误", e);
			throw new RuntimeException("游戏服务器启动失败", e);
		}
	}

	/**
	 * 加载配置
	 */
	private void loadConfiguration() {
		ConfigurationManager config = ConfigurationManager.getInstance();

		serverInfo = ServerManager.buildServerInfo(config, ServerType.Game);
		setCenter(config.getProperty("center"));
		setServerId(config.getInt("id", 0));

		logger.info("配置加载完成 - 服务器ID: {}, 中心服务器: {}", serverId, center);
	}

	/**
	 * 初始化组件
	 */
	private void initializeComponents() {
		executorPool = new ExecutorPool("Game");
		timer = new Timer().setRunners(executorPool);
		serverManager = new ServerManager(timer,
				ConfigurationManager.getInstance().getInt("plant", 0) != 0);
		logger.info("服务器组件初始化完成");
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

		new ServerService(0, GameClient.class).start(addresses);
		logger.info("网络服务启动成功, 地址: {}:{}", host, port);
	}

	/**
	 * 注册到中心服务器
	 */
	private void registerToCenter() {
		try {
			ClientProto.init();

			String[] centerAddress = center.split(":");
			if (centerAddress.length != 2) {
				throw new IllegalArgumentException("中心服务器地址格式错误: " + center);
			}

			serverManager.registerSever(centerAddress, HandleTypeRegister::parseMessage,
					ServerType.Center, serverId, serverInfo.getIpConfig().toStringUtf8(), ServerType.Game);

			logger.info("已向中心服务器注册, 地址: {}:{}", centerAddress[0], centerAddress[1]);

		} catch (Exception e) {
			logger.error("向中心服务器注册失败", e);
			throw new RuntimeException("中心服务器注册失败", e);
		}
	}

	/**
	 * 初始化游戏管理器
	 */
	private void initializeGameManagers() {
		tableManager = new TableManager();
		logger.info("游戏管理器初始化完成");
	}
}