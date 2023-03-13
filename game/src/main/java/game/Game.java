package game;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import game.client.GameClient;
import game.connect.ConnectProcessor;
import game.manager.TableManager;
import msg.ServerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.ServerManager;
import utils.config.ConfigurationManager;
import utils.config.ServerConfiguration;
import utils.utils.IpUtil;
import utils.utils.TimeUtil;

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

	private GameClient gateClient;

	private ServerManager serverManager;

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

	public void setServerManager(ServerManager serverManager) {
		this.serverManager = serverManager;
	}

	public GameClient getGateClient() {
		return gateClient;
	}

	public void setGateClient(GameClient gateClient) {
		this.gateClient = gateClient;
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

		new GameService(90).start(configuration.getHostList());

		//向注册中心注册
		registerToCenter();

		//每天 0 点 重置 桌子号参数
		resetTableIndex();

		LOGGER.info("[START] game server is start!!!");
	}

	/**
	 * 向注册中心注册
	 */
	private void registerToCenter() {
		setServerManager(new ServerManager());
		ServerManager serverManager = getServerManager();
		String[] ipPort = getCenter().split(":");

		serverManager.registerSever(ipPort, ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Game, getServerId(), getInnerIp() + ":" + getPort(),
				ServerType.Center);
	}

	/**
	 * 每天 0 点 重置 桌子号参数
	 */
	private void resetTableIndex() {
		long nextZero = TimeUtil.curZeroHourTime(System.currentTimeMillis()) + TimeUtil.DAY;
		registerTimer((int) nextZero / 1000, (int) TimeUtil.DAY / 1000, -1, game -> {
			TableManager.getInstance().resetTableId();
			return false;
		}, this);
	}

	public static void main(String[] args) {
		try {
			instance.start();
		} catch (Exception e) {
			LOGGER.error("failed for start game server!", e);
		}
	}
}
