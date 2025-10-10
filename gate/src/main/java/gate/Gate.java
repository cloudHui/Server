package gate;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import gate.client.ClientProto;
import gate.client.GateTcpClient;
import gate.connect.ConnectProcessor;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.connect.TCPConnect;
import net.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import threadtutil.thread.ExecutorPool;
import threadtutil.timer.Timer;
import utils.ServerManager;
import utils.config.ConfigurationManager;
import utils.other.IpUtil;

public class Gate {
	private final static Logger logger = LoggerFactory.getLogger(Gate.class);

	private final static Gate instance = new Gate();

	private final ExecutorPool executorPool;
	private final Timer timer;

	private int port;
	private int serverId;
	private String innerIp;
	private String center;

	private ServerManager serverManager;

	private Gate() {
		executorPool = new ExecutorPool("Gate");
		timer = new Timer().setRunners(executorPool);
	}

	public static Gate getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		try {
			instance.start();
		} catch (Exception e) {
			logger.error("[failed for start gate server!]", e);
		}
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

	public void setCenter(String center) {
		this.center = center;
	}

	public ServerManager getServerManager() {
		return serverManager;
	}

	public void execute(Runnable r) {
		executorPool.execute(r);
	}

	private void start() {

		ConfigurationManager cfgMgr = ConfigurationManager.getInstance();

		setPort(cfgMgr.getInt("port", 0));
		int wsPort = cfgMgr.getInt("wsGate", 0);

		setServerId(cfgMgr.getInt("id", 0));

		setCenter(cfgMgr.getProperty("center"));

		setInnerIp(IpUtil.getLocalIP());

		List<SocketAddress> addresses = new ArrayList<>();
		addresses.add(new InetSocketAddress(getInnerIp(), getPort()));
		new ServerService(90, GateTcpClient.class).start(addresses);
		addresses.clear();
		addresses.add(new InetSocketAddress(getInnerIp(), wsPort));
		serverManager = new ServerManager(timer, cfgMgr.getInt("plant", 0) != 0);
		new GateWsService().start(addresses);

		//向注册中心注册
		registerToCenter();

		logger.info("[gate server {}:{} is start!!!]", getInnerIp(), getPort());
	}

	/**
	 * 向注册中心注册
	 */
	private void registerToCenter() {
		ConnectProcessor.init();
		ClientProto.init();
		serverManager.registerSever(center.split(":"), null, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Center, getServerId(),
				getInnerIp() + ":" + getPort(),
				ServerType.Gate, new TCPConnect.CallParam(CMsg.REQ_SERVER, ModelProto.ReqServerInfo.newBuilder()
						.addServerType(ServerType.Game.getServerType())
						.addServerType(ServerType.Hall.getServerType())
						.addServerType(ServerType.Room.getServerType())
						.build()));
	}
}
