package room;

import msg.MessageId;
import msg.ServerType;
import net.connect.TCPConnect;
import net.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import room.client.RoomClient;
import room.connect.ConnectProcessor;
import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.ServerClientManager;
import utils.ServerManager;
import utils.config.ConfigurationManager;
import utils.config.ServerConfiguration;
import utils.utils.IpUtil;

public class Room {
	private final static Logger LOGGER = LoggerFactory.getLogger(Room.class);

	private final static Room instance = new Room();

	private final ExecutorPool executorPool;
	private final Timer timer;

	private int port;
	private int serverId;
	private String innerIp;
	private String center;

	public ServerClientManager serverClientManager = new ServerClientManager();


	private ServerManager serverManager;

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

	public static Room getInstance() {
		return instance;
	}


	private Room() {
		executorPool = new ExecutorPool("Game");
		timer = new Timer().setRunners(executorPool);
	}

	public <T> void registerTimer(int delay, int interval, int count, Runner<T> runner, T param) {
		timer.register(delay, interval, count, runner, param);
	}

	public void execute(Runnable r) {
		executorPool.execute(r);
	}

	public void serialExecute(Task t) {
		executorPool.serialExecute(t);
	}


	private void start() {

		ConfigurationManager cfgMgr = ConfigurationManager.getInstance();
		ServerConfiguration configuration = cfgMgr.getServers().get("room");
		if (null == configuration || !configuration.hasHostString()) {
			LOGGER.error("ERROR! failed for can not find server config");
			return;
		}

		setPort(cfgMgr.getInt("port", 0));

		setServerId(cfgMgr.getInt("id", 0));

		setCenter(cfgMgr.getProperty("center"));

		setInnerIp(IpUtil.getLocalIP());

		ServerService service = new ServerService(0, RoomClient.class).start(configuration.getHostList());
		setServerManager(new ServerManager(service.getWorkerGroup()));
		//向注册中心注册
		registerToCenter();

		LOGGER.info("[START] game server is start!!!");
	}

	/**
	 * 向注册中心注册
	 */
	private void registerToCenter() {
		String[] ipPort = getCenter().split(":");
		serverManager.registerSever(ipPort, ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Center, getServerId(), getInnerIp() + ":" + getPort(),
				ServerType.Room);
	}

	/**
	 * 获取其他除注册中心意外的所有服务端口ip
	 */
	private void getGameServer() {
		registerTimer(3000, 1000, -1, gate -> {
			TCPConnect serverClient = serverManager.getServerClient(ServerType.Center);
			if (serverClient != null) {
				ModelProto.ReqServerInfo.Builder req = ModelProto.ReqServerInfo.newBuilder();
				req.addServerType(ServerType.Game.getServerType());
				serverClient.sendMessage(MessageId.REQ_SERVER, req.build());
				return true;
			}
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
