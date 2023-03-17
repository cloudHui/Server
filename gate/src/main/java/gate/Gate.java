package gate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import gate.connect.ConnectProcessor;
import msg.Message;
import msg.ServerType;
import net.connect.TCPConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.ServerManager;
import utils.config.ConfigurationManager;
import utils.utils.IpUtil;

public class Gate {
	private final static Logger logger = LoggerFactory.getLogger(Gate.class);

	private final static Gate instance = new Gate();

	private final ExecutorPool executorPool;
	private final Timer timer;

	private int port;
	private String ip;
	private int serverId;
	private String innerIp;
	private String center;

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

	public static Gate getInstance() {
		return instance;
	}


	private Gate() {
		executorPool = new ExecutorPool("Gate");
		timer = new Timer().setRunners(executorPool);
	}

	public <T> void registerTimer(int delay, int interval, int count, Runner<T> runner, T param) {
		timer.register(delay, interval, count, runner, param);
	}

	public Future<?> execute(Runnable r) {
		return executorPool.execute(r);
	}

	public <T extends Task> CompletableFuture<T> serialExecute(T t) {
		return executorPool.serialExecute(t);
	}


	private void start() {

		ConfigurationManager cfgMgr = ConfigurationManager.INSTANCE().load();

		setPort(cfgMgr.getInt("port", 0));

		setIp(IpUtil.getOutIp());

		setServerId(cfgMgr.getInt("id", 0));

		setCenter(cfgMgr.getProperty("center"));

		setInnerIp(IpUtil.getLocalIP());

		new GateService().start(cfgMgr.getServers().get("gate").getHostList());

		new GateWsService().start(cfgMgr.getServers().get("wsGate").getHostList());
//		//向注册中心注册
//		registerToCenter();
//
//		//获取其他服务
//		getAllOtherServer();

		logger.info("[START] gate server is start!!!");
	}

	/**
	 * 向注册中心注册
	 */
	private void registerToCenter() {
		setServerManager(new ServerManager());
		ServerManager serverManager = getServerManager();
		String[] ipPort = getCenter().split(":");

		serverManager.registerSever(ipPort, ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Gate, getServerId(), getInnerIp() + ":" + getPort(),
				ServerType.Center);
	}

	/**
	 * 获取其他除注册中心意外的所有服务端口ip
	 */
	private void getAllOtherServer() {
		registerTimer(3000, 1000, -1, gate -> {
			TCPConnect serverClient = serverManager.getServerClient(ServerType.Center);
			if (serverClient != null) {
				ModelProto.ReqServerInfo.Builder req = ModelProto.ReqServerInfo.newBuilder();
				req.addServerType(ServerType.Game.getServerType());
				req.addServerType(ServerType.Hall.getServerType());
				serverClient.sendMessage(Message.REQ_SERVER, req.build(), null);
				return true;
			}
			return false;
		}, this);
	}


	public static void main(String[] args) {
		try {
			instance.start();
		} catch (Exception e) {
			logger.error("failed for start gate server!", e);
		}
	}
}
