package game;

import game.client.GameClient;
import game.connect.ConnectProcessor;
import game.manager.TableManager;
import msg.ServerType;
import net.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.ServerClientManager;
import utils.ServerManager;
import utils.SvnManager;
import utils.config.ConfigurationManager;
import utils.config.ServerConfiguration;
import utils.utils.IpUtil;

public class Game {
	private final static Logger LOGGER = LoggerFactory.getLogger(Game.class);

	private static final Game instance;

	private ExecutorPool executorPool;
	private Timer timer;

	private int port;
	private String ip;
	private int serverId;
	private String innerIp;
	private String center;


	static {
		instance = new Game();
		instance.executorPool = new ExecutorPool("Game");
		instance.timer = new Timer().setRunners(instance.executorPool);
	}


	private final ServerClientManager serverClientManager = new ServerClientManager();

	private ServerManager serverManager;

	private final SvnManager svnManager = new SvnManager();

	private TableManager tableManager;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
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
		ServerConfiguration configuration = cfgMgr.getServers().get("game");
		if (null == configuration || !configuration.hasHostString()) {
			LOGGER.error("[ERROR! failed for can not find server config]");
			return;
		}

		setPort(cfgMgr.getInt("port", 0));

		setIp(IpUtil.getOutIp());

		setServerId(cfgMgr.getInt("id", 0));

		setCenter(cfgMgr.getProperty("center"));

		setInnerIp(IpUtil.getLocalIP());

		ServerService service = new ServerService(0, GameClient.class).start(configuration.getHostList());
		serverManager = new ServerManager(service.getWorkerGroup());
		//向注册中心注册
		registerToCenter();

		//初始化
		init();

		//初始化代码管理
		if(cfgMgr.getInt("plant",0) !=0){
			initSvn();
		}
	}

	/**
	 * 向注册中心注册
	 */
	private void registerToCenter() {
		ServerManager serverManager = getServerManager();
		String[] ipPort = getCenter().split(":");

		serverManager.registerSever(ipPort, ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				null, ServerType.Center, getServerId(), getInnerIp() + ":" + getPort(),
				ServerType.Game);
	}

	/**
	 * 初始化桌子管理
	 */
	private void init() {
		tableManager = new TableManager();
	}

	/**
	 * 初始化svn 代码管理
	 */
	private void initSvn() {
		registerTimer(3000, 60000, -1, game -> {
			svnManager.jarUpdate();
			boolean update = svnManager.checkJarVersionUpdate();
			if (update) {
				svnManager.callBat();
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
			//System.setProperty("file.encoding", "UTF-8");
			//DingTalkWaring dingTalkWaring = new DingTalkWaring();
			//dingTalkWaring.sendMsg("我要测试", "17671292550");
			instance.start();
			//instance.testLog();
			LOGGER.info("[game server is start!!!] ");
		} catch (Exception e) {
			LOGGER.error("[failed for start game server!]", e);
		}
	}
}
