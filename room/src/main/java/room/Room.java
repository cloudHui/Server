package room;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.connect.TCPConnect;
import net.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import room.client.ClientProto;
import room.client.RoomClient;
import room.connect.ConnectProcessor;
import room.manager.RoomManager;
import threadtutil.thread.ExecutorPool;
import threadtutil.timer.Timer;
import utils.ServerClientManager;
import utils.ServerManager;
import utils.config.ConfigurationManager;
import utils.config.ServerConfiguration;

public class Room {
	private final static Logger LOGGER = LoggerFactory.getLogger(Room.class);

	private final static Room instance = new Room();

	private final ExecutorPool executorPool;
	private final Timer timer;
	private final ServerClientManager serverClientManager = new ServerClientManager();
	private int serverId;
	private String center;
	/**
	 * 本服务信息
	 */
	private ModelProto.ServerInfo.Builder serverInfo;
	private ServerManager serverManager;

	private Room() {
		executorPool = new ExecutorPool("Room");
		timer = new Timer().setRunners(executorPool);
	}

	public static Room getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		try {
			instance.start();
			//instance.initJar();
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

	public ServerManager getServerManager() {
		return serverManager;
	}

	public ModelProto.ServerInfo.Builder getServerInfo() {
		return serverInfo;
	}

	public ServerClientManager getServerClientManager() {
		return serverClientManager;
	}

	public void execute(Runnable r) {
		executorPool.execute(r);
	}

	private void start() {

		ConfigurationManager cfgMgr = ConfigurationManager.getInstance();
		ServerConfiguration configuration = cfgMgr.getServers().get("room");
		if (null == configuration || !configuration.hasHostString()) {
			LOGGER.error("[ERROR! failed for can not find server config]");
			return;
		}
		serverInfo = ServerManager.buildServerInfo(cfgMgr, ServerType.Room);
		setServerId(cfgMgr.getInt("id", 0));

		setCenter(cfgMgr.getProperty("center"));

		List<SocketAddress> addresses = new ArrayList<>();
		String[] split = serverInfo.getIpConfig().toStringUtf8().split(":");
		addresses.add(new InetSocketAddress(split[0], Integer.parseInt(split[1])));
		new ServerService(0, RoomClient.class).start(addresses);
		serverManager = new ServerManager(timer, cfgMgr.getInt("plant", 0) != 0);

		//向注册中心注册
		registerToCenter();
		RoomManager.getInstance().init();
		LOGGER.info("[room server {}:{} is start!!!]", split[0], Integer.parseInt(split[1]));
	}

	/**
	 * 向注册中心注册
	 */
	private void registerToCenter() {
		ClientProto.init();
		ConnectProcessor.init();
		serverManager.registerSever(center.split(":"), ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Center, getServerId(), serverInfo.getIpConfig().toStringUtf8(),
				ServerType.Room, new TCPConnect.CallParam(CMsg.REQ_SERVER, ModelProto.ReqServerInfo.newBuilder()
						.addServerType(ServerType.Game.getServerType())
						.build()));
	}
}
