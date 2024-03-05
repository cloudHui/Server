package game;

import com.alibaba.fastjson.JSON;
import game.client.GameClient;
import game.connect.ConnectProcessor;
import game.manager.TableManager;
import monitor.ServerMonitor;
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
import utils.utils.DingTalkWaring;
import utils.utils.IpUtil;

public class Game {
	private final static Logger LOGGER = LoggerFactory.getLogger(Game.class);

	private final static Game instance = new Game();

	private final ExecutorPool executorPool;
	private final Timer timer;

	private int port;
	private String ip;
	private int serverId;
	private String innerIp;
	private String center;

	public ServerClientManager serverClientManager = new ServerClientManager();

	private final ServerManager serverManager = new ServerManager();

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
		executorPool = new ExecutorPool("Game");
		timer = new Timer().setRunners(executorPool);
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
			LOGGER.error("ERROR! failed for can not find server config");
			return;
		}

		setPort(cfgMgr.getInt("port", 0));

		setIp(IpUtil.getOutIp());

		setServerId(cfgMgr.getInt("id", 0));

		setCenter(cfgMgr.getProperty("center"));

		setInnerIp(IpUtil.getLocalIP());

		new ServerService(0, GameClient.class).start(configuration.getHostList());

		//向注册中心注册
		registerToCenter();

		//初始化
		init();

		//初始化代码管理
		initSvn();

		LOGGER.info("[START] game server is start!!!");
	}

	/**
	 * 向注册中心注册
	 */
	private void registerToCenter() {
		ServerManager serverManager = getServerManager();
		String[] ipPort = getCenter().split(":");

		serverManager.registerSever(ipPort, ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Game, getServerId(), getInnerIp() + ":" + getPort(),
				ServerType.Center, 0);
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

	public static void main(String[] args) {
		try {
			DingTalkWaring dingTalkWaring = new DingTalkWaring();
			dingTalkWaring.sendMsg("我要测试", "17671292550");
			//instance.start();
		} catch (Exception e) {
			LOGGER.error("failed for start game server!", e);
		}
	}
}
