package robot;

import com.google.protobuf.Message;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.connect.handle.ConnectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import robot.connect.ConnectProcessor;
import robot.connect.handle.RobotHandleManager;
import threadtutil.thread.ExecutorPool;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.ServerManager;
import utils.config.ConfigurationManager;
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

	private Robot() {
		executorPool = new ExecutorPool("Robot");
		timer = new Timer().setRunners(executorPool);
	}

	public static Robot getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		try {
			instance.start();
		} catch (Exception e) {
			LOGGER.error("[failed for start robot server!]", e);
			System.exit(0);
		}
	}

	public String getCenter() {
		return center;
	}

	public void setCenter(String center) {
		this.center = center;
	}

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

	public ServerManager getServerManager() {
		return serverManager;
	}

	public void execute(Runnable r) {
		executorPool.execute(r);
	}

	public <T> void registerTimer(int delay, int interval, int count, Runner<T> runner, T param) {
		timer.register(delay, interval, count, runner, param);
	}

	private void start() {
		RobotHandleManager.init();
		ConfigurationManager cfgMgr = ConfigurationManager.getInstance();
		serverManager = new ServerManager(timer, cfgMgr.getInt("plant", 0) != 0);
		ConnectProcessor.init();
		setPort(cfgMgr.getInt("port", 0));

		setServerId(cfgMgr.getInt("id", 0));

		setCenter(cfgMgr.getProperty("center"));

		setInnerIp(IpUtil.getLocalIP());

		//向注册中心注册
		registerToCenter();

		//获取其他服务
		getGateServer();

		LOGGER.info("[robot server is start!!!]");
	}

	/**
	 * 向注册中心注册
	 */
	private void registerToCenter() {
		String[] ipPort = getCenter().split(":");
		serverManager.registerSever(ipPort, ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Center, getServerId(),
				getInnerIp() + ":" + getPort(),
				ServerType.Robot);
	}

	/**
	 * 获取gate
	 */
	private void getGateServer() {
		registerTimer(3000, 1000, -1, robot -> {
			ConnectHandler serverClient = serverManager.getServerClient(ServerType.Center);
			if (serverClient != null) {
				ModelProto.ReqServerInfo.Builder req = ModelProto.ReqServerInfo.newBuilder();
				req.addServerType(ServerType.Gate.getServerType());
				RobotHandleManager.sendMsg(serverClient, req.build(), CMsg.REQ_SERVER);
				return true;
			}
			return false;
		}, this);
	}

	/**
	 * 发送消息
	 */
	public void getClientSendMessage(ServerType serverType, int msgId, Message message) {
		registerTimer(0, 1000, -1, gate -> {
			ConnectHandler serverClient = Robot.getInstance().getServerManager().getServerClient(serverType);
			if (serverClient != null) {
				RobotHandleManager.sendMsg(serverClient, message, msgId);
				return true;
			}
			return false;
		}, this);
	}
}
