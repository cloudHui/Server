package game;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

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
import utils.config.ConfigurationManager;
import utils.config.ServerConfiguration;
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

	private ServerManager serverManager = new ServerManager();

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

	public <T> void registerTimer(int delay, int interval, int count, Runner<T> runner, T param) {
		timer.register(delay, interval, count, runner, param);
	}

	public <T> void registerSerialTimer(int groupId, int delay, int interval, int count, Runner<T> runner, T param) {
		timer.registerSerial(groupId, delay, interval, count, runner, param);
	}

	public Future<?> execute(Runnable r) {
		return executorPool.execute(r);
	}

	public <T extends Task> CompletableFuture<T> serialExecute(T t) {
		return executorPool.serialExecute(t);
	}


	private void start() {

		ConfigurationManager cfgMgr = ConfigurationManager.INSTANCE().load();
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
	 * 初始化
	 */
	private void init() {
		tableManager = new TableManager();
	}

	public static void main(String[] args) {
		try {
			instance.start();
		} catch (Exception e) {
			LOGGER.error("failed for start game server!", e);
		}
	}
}
