package robot;

import java.util.concurrent.atomic.AtomicInteger;

import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.connect.TCPConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import proto.ServerProto;
import robot.connect.ConnectProcessor;
import threadtutil.thread.ExecutorPool;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.ServerManager;
import utils.config.ConfigurationManager;
import utils.manager.HandleManager;
import utils.other.IpUtil;

public class Robot {
	private final static Logger LOGGER = LoggerFactory.getLogger(Robot.class);

	private final static Robot instance = new Robot();
	private final ExecutorPool executorPool;
	private final Timer timer;

	private ServerManager serverManager;

	private String center;
	private int port;
	private int serverId;
	private String innerIp;

	private static AtomicInteger id = new AtomicInteger(0);

	public static String getId() {
		synchronized (Robot.class) {
			int i = id.incrementAndGet();
			if (i >= Integer.MAX_VALUE) {
				LOGGER.warn("[ID counter reached maximum value, resetting to 1]");
				id = new AtomicInteger(1);
				return 1 + "";
			}
			LOGGER.debug("[Generated new ID: {}]", i);
			return i + "";
		}
	}

	private Robot() {
		LOGGER.debug("[Initializing Robot instance]");
		executorPool = new ExecutorPool("Robot");
		timer = new Timer().setRunners(executorPool);
		LOGGER.debug("[Robot instance initialized successfully]");
	}

	public static Robot getInstance() {
		LOGGER.debug("[Getting Robot singleton instance]");
		return instance;
	}

	public static void main(String[] args) {
		try {
			LOGGER.info("[Starting Robot server application]");
			LOGGER.debug("[Command line arguments count: {}]", args.length);
			if (args.length > 0) {
				LOGGER.debug("[Command line arguments: {}]", String.join(", ", args));
			}
			instance.start();
			LOGGER.info("[Robot server started successfully]");
		} catch (Exception e) {
			LOGGER.error("[Failed to start robot server!]", e);
			System.exit(1);
		}
	}

	public void setCenter(String center) {
		LOGGER.debug("[Setting center address: {}]", center);
		this.center = center;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		LOGGER.debug("[Setting port: {}]", port);
		this.port = port;
	}

	public int getServerId() {
		return serverId;
	}

	public void setServerId(int serverId) {
		LOGGER.debug("[Setting server ID: {}]", serverId);
		this.serverId = serverId;
	}

	public String getInnerIp() {
		return innerIp;
	}

	public void setInnerIp(String innerIp) {
		LOGGER.debug("[Setting inner IP: {}]", innerIp);
		this.innerIp = innerIp;
	}

	public ServerManager getServerManager() {
		LOGGER.debug("[Getting server manager instance]");
		return serverManager;
	}

	public void execute(Runnable r) {
		LOGGER.debug("[Executing runnable task: {}]", r.getClass().getSimpleName());
		executorPool.execute(r);
	}

	public <T> void registerTimer(long delay, long interval, int count, Runner<T> runner, T param) {
		LOGGER.debug("[Registering timer - delay: {}, interval: {}, count: {}, runner: {}]",
				delay, interval, count, runner.getClass().getSimpleName());
		timer.register(delay, interval, count, runner, param);
	}

	private void start() {
		LOGGER.info("[Starting Robot server initialization]");

		try {
			ConfigurationManager cfgMgr = ConfigurationManager.getInstance();
			if (cfgMgr == null) {
				LOGGER.error("[ConfigurationManager instance is null!]");
				throw new IllegalStateException("ConfigurationManager not initialized");
			}
			LOGGER.debug("[ConfigurationManager instance obtained successfully]");

			boolean plantFlag = cfgMgr.getInt("plant", 0) != 0;
			LOGGER.debug("[Plant configuration flag: {}]", plantFlag);
			serverManager = new ServerManager(timer, plantFlag);
			LOGGER.debug("[ServerManager initialized with plant flag: {}]", plantFlag);

			int portConfig = cfgMgr.getInt("port", 0);
			if (portConfig <= 0 || portConfig > 65535) {
				LOGGER.warn("[Invalid port configuration: {}, using default or current value]", portConfig);
			} else {
				setPort(portConfig);
			}

			int serverIdConfig = cfgMgr.getInt("id", 0);
			if (serverIdConfig <= 0) {
				LOGGER.warn("[Invalid server ID configuration: {}]", serverIdConfig);
			} else {
				setServerId(serverIdConfig);
			}

			String centerConfig = cfgMgr.getProperty("center");
			if (centerConfig == null || centerConfig.trim().isEmpty()) {
				LOGGER.error("[Center configuration is null or empty!]");
				throw new IllegalArgumentException("Center configuration is required");
			}
			setCenter(centerConfig);

			String localIP = IpUtil.getLocalIP();
			if (localIP == null || localIP.trim().isEmpty()) {
				LOGGER.warn("[Failed to get valid local IP address]");
			} else {
				setInnerIp(localIP);
				LOGGER.debug("[Local IP address obtained: {}]", localIP);
			}

			LOGGER.info("[Robot configuration summary - Port: {}, Server ID: {}, Center: {}, Inner IP: {}]",
					port, serverId, center, innerIp);

			// 向注册中心注册
			registerToCenter();

			LOGGER.info("[Robot server started successfully!]");
		} catch (Exception e) {
			LOGGER.error("[Failed to initialize Robot server!]", e);
			throw e;
		}
	}

	/**
	 * 向注册中心注册
	 */
	private void registerToCenter() {
		LOGGER.info("[Starting registration to center]");

		try {
			if (center == null || center.trim().isEmpty()) {
				LOGGER.error("[Center address is not set, cannot register!]");
				return;
			}

			String[] centerParts = center.split(":");
			if (centerParts.length != 2) {
				LOGGER.error("[Invalid center address format: {}, expected 'host:port']", center);
				return;
			}

			LOGGER.debug("[Initializing ConnectProcessor]");
			ConnectProcessor.init();

			LOGGER.debug("[Initializing HandleManager]");
			HandleManager.init(ConnectProcessor.class);

			String serverAddress = getInnerIp() + ":" + getPort();
			LOGGER.info("[Registering server - ID: {}, Address: {}, Center: {}]",
					getServerId(), serverAddress, center);

			ServerProto.ReqServerInfo serverInfo = ServerProto.ReqServerInfo.newBuilder()
					.addServerType(ServerType.Gate.getServerType())
					.build();
			LOGGER.debug("[Created server info with server type: {}]", ServerType.Gate.getServerType());

			TCPConnect.CallParam callParam = new TCPConnect.CallParam(
					CMsg.REQ_SERVER,
					serverInfo,
					HandleManager::sendMsg,
					ConnectProcessor.PARSER
			);

			serverManager.registerSever(
					centerParts,
					ConnectProcessor.TRANSFER,
					ConnectProcessor.PARSER,
					ConnectProcessor.HANDLERS,
					ServerType.Center,
					getServerId(),
					serverAddress,
					ServerType.Robot,
					callParam
			);

			LOGGER.info("[Registration to center initiated successfully]");
		} catch (Exception e) {
			LOGGER.error("[Failed to register to center!]", e);
		}
	}
}