package game;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import game.client.ClientProto;
import game.client.GameClient;
import game.connect.ConnectProcessor;
import game.manager.TableManager;
import msg.registor.enums.ServerType;
import net.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.GitJarManager;
import utils.ServerClientManager;
import utils.ServerManager;
import utils.config.ConfigurationManager;

public class Game {
	private final static Logger LOGGER = LoggerFactory.getLogger(Game.class);

	private static final Game instance;

	private ExecutorPool executorPool;
	private Timer timer;

	private int serverId;
	private String center;

	/**
	 * 本服务信息
	 */
	private ModelProto.ServerInfo.Builder serverInfo;


	static {
		instance = new Game();
		instance.executorPool = new ExecutorPool("Game");
		instance.timer = new Timer().setRunners(instance.executorPool);
	}


	private final ServerClientManager serverClientManager = new ServerClientManager();

	private ServerManager serverManager;

	private TableManager tableManager;

	private GitJarManager gitJar;

	public int getServerId() {
		return serverId;
	}

	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

	public String getCenter() {
		return center;
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

	public void setTableManager(TableManager tableManager) {
		this.tableManager = tableManager;
	}

	public ModelProto.ServerInfo.Builder getServerInfo() {
		return serverInfo;
	}


	public static Game getInstance() {
		return instance;
	}

	private Game() {
	}

	public <T> void registerTimer(long delay, long interval, int count, Runner<T> runner, T param) {
		timer.register(delay, interval, count, runner, param);
	}

	public <T> void registerSerialTimer(int groupId, long delay, long interval, int count, Runner<T> runner, T param) {
		timer.registerSerial(groupId, delay, interval, count, runner, param);
	}

	/**
	 * 直接提交处理
	 */
	public void execute(Runnable r) {
		executorPool.execute(r);
	}

	/**
	 * 按顺序有序处理
	 */
	public void serialExecute(Task t) {
		executorPool.serialExecute(t);
	}


	private void start() {

		ConfigurationManager cfgMgr = ConfigurationManager.getInstance();

		serverInfo = ServerManager.manageServerInfo(cfgMgr, ServerType.Game);

		setCenter(cfgMgr.getProperty("center"));

		setServerId(cfgMgr.getInt("id", 0));
		List<SocketAddress> addresses = new ArrayList<>();
		String[] split = serverInfo.getIpConfig().toStringUtf8().split(":");
		addresses.add(new InetSocketAddress(split[0], Integer.parseInt(split[1])));
		new ServerService(0, GameClient.class).start(addresses);
		serverManager = new ServerManager();
		//向注册中心注册
		registerToCenter();

		//初始化
		init();

		ClientProto.init();
		//初始化代码管理
		if (cfgMgr.getInt("plant", 0) != 0) {
			initJar();
		}
		LOGGER.info("[game server {}:{}  is start!!!] ", split[0], Integer.parseInt(split[1]));
	}

	/**
	 * 向注册中心注册
	 */
	private void registerToCenter() {
		ConnectProcessor.init();
		ServerManager serverManager = getServerManager();
		String[] ipPort = getCenter().split(":");
		serverManager.registerSever(ipPort, ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Center, getServerId(), serverInfo.getIpConfig().toStringUtf8(),
				ServerType.Game);
	}

	/**
	 * 初始化桌子管理
	 */
	private void init() {
		tableManager = new TableManager();
	}

	/**
	 * 初始化Jar 代码管理
	 */
	private void initJar() {
		gitJar = new GitJarManager();
		registerTimer(3 * 1000, 60 * 1000, -1, game -> {
			boolean update = gitJar.checkJarUpdate();
			if (update) {
				gitJar.callBat();
				System.exit(0);
			}
			return false;
		}, this);
	}

	/**
	 * 测试日志
	 */
	private void testLog() {
		LOGGER.info("in testlog");
		ConfigurationManager cfgMgr = ConfigurationManager.getInstance();
		LOGGER.info(cfgMgr.getServers().toString());
		registerTimer(1, 30000, -1, game -> {
			LOGGER.info("game server 测试日志");
			return false;
		}, this);
	}

	public static void main(String[] args) {
		try {
			instance.start();
			//instance.initJar();
		} catch (Exception e) {
			LOGGER.error("[failed for start game server!]", e);
		}
	}
}
