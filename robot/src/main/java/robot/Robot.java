package robot;

import java.util.UUID;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import msg.HallMessageId;
import msg.MessageId;
import msg.ServerType;
import net.connect.handle.ConnectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import proto.ModelProto;
import robot.connect.ConnectProcessor;
import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
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
	private String ip;
	private int serverId;
	private String innerIp;

	public static Robot getInstance() {
		return instance;
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

	public ServerManager getServerManager() {
		return serverManager;
	}

	private Robot() {
		executorPool = new ExecutorPool("Robot");
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
		serverManager = new ServerManager();
		ConnectProcessor.init();
		ConfigurationManager cfgMgr = ConfigurationManager.getInstance();
		setPort(cfgMgr.getInt("port", 0));
		setIp(IpUtil.getOutIp());

		setServerId(cfgMgr.getInt("id", 0));

		setCenter(cfgMgr.getProperty("center"));

		setInnerIp(IpUtil.getLocalIP());

		//向注册中心注册
		registerToCenter();

		//获取其他服务
		getAllOtherServer();

		login();
		LOGGER.info("[robot server is start!!!]");
	}

	/**
	 * 向注册中心注册
	 */
	private void registerToCenter() {
		String[] ipPort = getCenter().split(":");
		int plant = ConfigurationManager.getInstance().getInt("plant", 0);
		serverManager.registerSever(ipPort, ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Center, getServerId(),
				plant == 2 ? getIp() + ":" + getPort() : getInnerIp() + ":" + getPort(),
				ServerType.Gate);
	}

	/**
	 * 获取其他除注册中心以外的所有服务端口ip
	 */
	private void getAllOtherServer() {
		registerTimer(3000, 1000, -1, robot -> {
			ConnectHandler serverClient = serverManager.getServerClient(ServerType.Center);
			if (serverClient != null) {
				ModelProto.ReqServerInfo.Builder req = ModelProto.ReqServerInfo.newBuilder();
				req.addServerType(ServerType.Game.getServerType());
				req.addServerType(ServerType.Hall.getServerType());
				req.addServerType(ServerType.Room.getServerType());
				serverClient.sendMessage(MessageId.REQ_SERVER, req.build());
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
				serverClient.sendMessage(msgId, message);
				return true;
			}
			return false;
		}, this);
	}

	/**
	 * 模拟登录
	 */
	private void login() {
		HallProto.ReqLogin build = HallProto.ReqLogin.newBuilder().setNickName(ByteString.copyFromUtf8(UUID.randomUUID().toString())).build();
		getClientSendMessage(ServerType.Hall, HallMessageId.REQ_LOGIN_MSG, build);
	}

	public static void main(String[] args) {
		try {
			instance.start();
		} catch (Exception e) {
			LOGGER.error("[failed for start robot server!]", e);
			System.exit(0);
		}
	}
}
