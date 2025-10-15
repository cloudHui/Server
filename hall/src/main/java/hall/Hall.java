package hall;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import hall.client.ClientProto;
import hall.client.HallClient;
import hall.connect.ConnectProcessor;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.connect.TCPConnect;
import net.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import threadtutil.thread.ExecutorPool;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.ServerClientManager;
import utils.ServerManager;
import utils.config.ConfigurationManager;
import utils.manager.HandleManager;
import utils.other.IpUtil;

public class Hall {
	private final static Logger LOGGER = LoggerFactory.getLogger(Hall.class);

	private final static Hall instance = new Hall();

	private final ExecutorPool executorPool;
	private final Timer timer;
	public ServerClientManager serverClientManager = new ServerClientManager();
	private int serverId;
	private String center;
	private String innerIp;
	private int port;
	/**
	 * 本服务信息
	 */
	private ModelProto.ServerInfo.Builder serverInfo;
	private ServerManager serverManager;

	private Hall() {
		executorPool = new ExecutorPool("Hall");
		timer = new Timer().setRunners(executorPool);
	}

	public static Hall getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		try {
			instance.start();
		} catch (Exception e) {
			LOGGER.error("[failed for start game server!]", e);
		}
	}

	public int getServerId() {
		return serverId;
	}

	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

	public void setCenter(String center) {
		this.center = center;
	}

	public String getInnerIp() {
		return innerIp;
	}

	public void setInnerIp(String innerIp) {
		this.innerIp = innerIp;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public ServerManager getServerManager() {
		return serverManager;
	}

	public ModelProto.ServerInfo.Builder getServerInfo() {
		return serverInfo;
	}

	public void execute(Runnable r) {
		executorPool.execute(r);
	}

	public <T> void registerTimer(long delay, long interval, int count, Runner<T> runner, T param) {
		timer.register(delay, interval, count, runner, param);
	}

	private void start() {

		ConfigurationManager cfgMgr = ConfigurationManager.getInstance();

		serverInfo = ServerManager.buildServerInfo(cfgMgr, ServerType.Hall);
		setServerId(cfgMgr.getInt("id", 0));
		setInnerIp(IpUtil.getLocalIP());
		setPort(cfgMgr.getInt("port", 0));
		setCenter(cfgMgr.getProperty("center"));
		List<SocketAddress> addresses = new ArrayList<>();
		String[] split = serverInfo.getIpConfig().toStringUtf8().split(":");
		addresses.add(new InetSocketAddress(split[0], Integer.parseInt(split[1])));
		new ServerService(0, HallClient.class).start(addresses);
		serverManager = new ServerManager(timer, cfgMgr.getInt("plant", 0) != 0);

		//向注册中心注册
		registerToCenter();
		LOGGER.info("[hall server {}:{} is start!!!]", split[0], Integer.parseInt(split[1]));
	}

	/**
	 * 向注册中心注册
	 */
	private void registerToCenter() {
		ClientProto.init();
		ConnectProcessor.init();
		HandleManager.init(ConnectProcessor.class);
		serverManager.registerSever(center.split(":"), ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Center, getServerId(), serverInfo.getIpConfig().toStringUtf8(),
				ServerType.Hall, new TCPConnect.CallParam(CMsg.REQ_SERVER, ModelProto.ReqServerInfo.newBuilder()
						.addServerType(ServerType.Room.getServerType())
						.build(), HandleManager::sendMsg, ConnectProcessor.PARSER));
	}
}
