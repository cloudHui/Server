package lobby;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lobby.admin.LobbyAdminHttp;
import lobby.client.ClientProto;
import lobby.client.LobbyClient;
import lobby.connect.ConnectProcessor;
import lobby.db.InviteRepository;
import lobby.db.SqliteDatabase;
import lobby.db.UserEntity;
import lobby.db.UserRepository;
import lobby.manager.table.TableManager;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.connect.TCPConnect;
import net.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import proto.ServerProto;
import threadtutil.thread.ExecutorPool;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import tools.ServerClientManager;
import tools.ServerManager;
import utils.config.ConfigurationManager;
import tools.manager.HandleManager;
import utils.metrics.MetricsCollector;
import utils.metrics.MetricsHttpServer;
import utils.other.IpUtil;
import utils.other.MD5Utils;

/**
 * Lobby 服务器（原 hall + room 合并）
 */
public class Lobby {
	private static final Logger logger = LoggerFactory.getLogger(Lobby.class);
	private static final Lobby instance = new Lobby();

	private final ExecutorPool executorPool;
	private final Timer timer;
	public final ServerClientManager serverClientManager = new ServerClientManager();

	private int serverId;
	private String center;
	private String innerIp;
	private int port;
	private boolean openRegister;
	private ModelProto.ServerInfo serverInfo;
	private ServerManager serverManager;
	private MetricsHttpServer metricsHttpServer;
	private UserRepository userRepository;
	private InviteRepository inviteRepository;
	private LobbyAdminHttp adminHttp;

	private Lobby() {
		executorPool = new ExecutorPool("Lobby");
		timer = new Timer().setRunners(executorPool);
	}

	public static Lobby getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		try {
			logger.info("正在启动 Lobby 服务器...");
			instance.start();
			logger.info("Lobby 服务器启动成功");
		} catch (Exception e) {
			logger.error("Lobby 服务器启动失败", e);
			System.exit(1);
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

	public boolean isOpenRegister() {
		return openRegister;
	}

	public ServerManager getServerManager() {
		return serverManager;
	}

	public ServerClientManager getServerClientManager() {
		return serverClientManager;
	}

	public ModelProto.ServerInfo getServerInfo() {
		return serverInfo;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public InviteRepository getInviteRepository() {
		return inviteRepository;
	}

	public void execute(Runnable task) {
		executorPool.execute(task);
	}

	public <T> void registerTimer(long delay, long interval, int count, Runner<T> runner, T param) {
		timer.register(delay, interval, count, runner, param);
	}

	private void start() {
		loadConfiguration();
		initializeComponents();
		startNetworkService();
		registerToCenter();
		initializeRoomManager();
		startMetricsServer();
		startAdminHttp();
		logger.info("Lobby 启动完成! 服务器ID: {}, 地址: {}:{}, openRegister: {}",
				serverId, innerIp, port, openRegister);
	}

	private void startAdminHttp() {
		int adminPort = ConfigurationManager.getInstance().getInt("lobby.admin-http-port", 5701);
		try {
			adminHttp = new LobbyAdminHttp();
			adminHttp.start(adminPort);
		} catch (Exception e) {
			logger.error("Lobby admin HTTP 启动失败, port={}", adminPort, e);
		}
	}

	private void loadConfiguration() {
		ConfigurationManager config = ConfigurationManager.getInstance();
		serverInfo = ServerManager.buildServerInfo(config, ServerType.Lobby);
		setServerId(config.getInt("id", 0));
		setInnerIp(IpUtil.getLocalIP());
		setPort(config.getInt("port", 5700));
		setCenter(config.getProperty("center"));
		String openReg = config.getProperty("lobby.open-register");
		openRegister = openReg != null && Boolean.parseBoolean(openReg);
		logger.info("配置加载完成 - 服务器ID: {}, 端口: {}, 中心: {}, openRegister: {}",
				serverId, port, center, openRegister);
	}

	private void initializeComponents() {
		serverManager = new ServerManager(timer,
				ConfigurationManager.getInstance().getInt("plant", 0) != 0);

		SqliteDatabase db = SqliteDatabase.getInstance();
		db.initSchema();
		userRepository = new UserRepository(db);
		inviteRepository = new InviteRepository(db);
		seedDefaultData();
		logger.info("SQLite 与组件初始化完成");
	}

	private void seedDefaultData() {
		if (userRepository.countUsers() == 0) {
			UserEntity admin = new UserEntity();
			admin.setUsername("admin");
			admin.setNickname("admin");
			admin.setPasswordHash(MD5Utils.MD5("admin123"));
			admin.setEnabled(true);
			admin.setCreatedAt(System.currentTimeMillis());
			long id = userRepository.insert(admin);
			logger.info("已创建默认管理员账号 admin/admin123, userId={}", id);
		}
		if (inviteRepository.countInvites() == 0) {
			long expires = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000;
			lobby.db.InviteEntity seed = inviteRepository.create("seed invite", "system", expires, 10);
			if (seed != null) {
				logger.info("已创建默认邀请码（7天/10次）token={}", seed.getToken());
			}
		}
	}

	private void startNetworkService() {
		String[] addressParts = serverInfo.getIpConfig().toStringUtf8().split(":");
		if (addressParts.length != 2) {
			throw new RuntimeException("服务器地址格式错误: " + serverInfo.getIpConfig().toStringUtf8());
		}
		String host = addressParts[0];
		int listenPort = Integer.parseInt(addressParts[1]);
		List<SocketAddress> addresses = new ArrayList<>();
		addresses.add(new InetSocketAddress(host, listenPort));
		new ServerService(0, LobbyClient.class).start(addresses);
		logger.info("网络服务启动成功, 地址: {}:{}", host, listenPort);
	}

	private void registerToCenter() {
		try {
			ClientProto.init();
			ConnectProcessor.init();
			HandleManager.init(ClientProto.class);
			HandleManager.init(ConnectProcessor.class);

			String[] centerAddress = center.split(":");
			if (centerAddress.length != 2) {
				throw new IllegalArgumentException("中心服务器地址格式错误: " + center);
			}

			ServerProto.ReqServerInfo serverInfoRequest = ServerProto.ReqServerInfo.newBuilder()
					.addServerType(ServerType.Game.getServerType())
					.build();

			serverManager.registerSever(centerAddress, ConnectProcessor.TRANSFER,
					ConnectProcessor.PARSER, ConnectProcessor.HANDLERS,
					ServerType.Center, serverId, serverInfo.getIpConfig().toStringUtf8(),
					ServerType.Lobby, new TCPConnect.CallParam(CMsg.REQ_SERVER, serverInfoRequest,
							HandleManager::sendMsg, ConnectProcessor.PARSER));

			logger.info("已向中心服务器注册, 地址: {}:{}", centerAddress[0], centerAddress[1]);
		} catch (Exception e) {
			logger.error("向中心服务器注册失败", e);
			throw new RuntimeException("中心服务器注册失败", e);
		}
	}

	private void initializeRoomManager() {
		TableManager.getInstance().init();
		logger.info("房间管理器初始化完成");
	}

	private void startMetricsServer() {
		int metricsPort = ConfigurationManager.getInstance().getInt("metrics.port", 0);
		if (metricsPort > 0) {
			MetricsCollector.getInstance().setServiceName("lobby");
			metricsHttpServer = new MetricsHttpServer();
			metricsHttpServer.start(metricsPort);
		}
	}

	public static String newToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
