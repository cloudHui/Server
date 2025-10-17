package center;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import center.client.CenterClient;
import center.client.ClientProto;
import msg.registor.enums.ServerType;
import net.service.ServerService;
import proto.ModelProto;
import utils.ServerClientManager;
import utils.ServerManager;
import utils.config.ConfigurationManager;

/**
 * 中心服务器主类
 * 负责服务注册、发现和负载均衡
 */
public class Center {
	private static final Logger logger = LoggerFactory.getLogger(Center.class);
	private static final Center instance = new Center();

	private final ServerClientManager serverManager = new ServerClientManager();
	private ModelProto.ServerInfo.Builder serverInfo;

	private Center() {
		// 私有构造函数，单例模式
	}

	public static Center getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		try {
			logger.info("正在启动中心服务器...");
			instance.start();
			logger.info("中心服务器启动成功");
		} catch (Exception e) {
			logger.error("中心服务器启动失败", e);
			System.exit(1);
		}
	}

	public ServerClientManager getServerManager() {
		return serverManager;
	}

	public ModelProto.ServerInfo.Builder getServerInfo() {
		return serverInfo;
	}

	/**
	 * 启动中心服务器
	 */
	private void start() {
		try {
			ConfigurationManager config = ConfigurationManager.getInstance();

			// 初始化协议处理器
			ClientProto.init();

			// 构建服务器信息
			serverInfo = ServerManager.buildServerInfo(config, ServerType.Center);

			// 启动TCP服务
			startTcpService();

			// 启动HTTP服务
			startHttpService(config);
		} catch (Exception e) {
			logger.error("中心服务器启动过程中发生错误", e);
			throw new RuntimeException("中心服务器启动失败", e);
		}
	}

	/**
	 * 启动TCP服务
	 */
	private void startTcpService() {
		try {
			String[] addressParts = serverInfo.getIpConfig().toStringUtf8().split(":");
			if (addressParts.length != 2) {
				throw new IllegalArgumentException("服务器地址格式错误: " + serverInfo.getIpConfig().toStringUtf8());
			}

			String host = addressParts[0];
			int port = Integer.parseInt(addressParts[1]);

			List<SocketAddress> addresses = new ArrayList<>();
			addresses.add(new InetSocketAddress(host, port));

			new ServerService(0, CenterClient.class).start(addresses);
			logger.info("中心服务器TCP服务启动成功, 地址: {}:{}", host, port);

		} catch (Exception e) {
			logger.error("启动TCP服务失败", e);
			throw new RuntimeException("TCP服务启动失败", e);
		}
	}

	/**
	 * 启动HTTP服务
	 */
	private void startHttpService(ConfigurationManager config) {
		try {
			String[] addressParts = serverInfo.getIpConfig().toStringUtf8().split(":");
			String host = addressParts[0];
			int httpPort = config.getInt("httpPort", 0);

			if (httpPort <= 0) {
				logger.warn("HTTP端口未配置或配置错误，跳过HTTP服务启动");
				return;
			}

			new CenterHttpService().start(new InetSocketAddress(host, httpPort));
			logger.info("中心服务器HTTP服务启动成功, 地址: {}:{}", host, httpPort);

		} catch (Exception e) {
			logger.error("启动HTTP服务失败", e);
			throw new RuntimeException("HTTP服务启动失败", e);
		}
	}
}