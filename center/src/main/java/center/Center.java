package center;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import center.client.CenterClient;
import center.client.ClientProto;
import msg.registor.enums.ServerType;
import net.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import utils.ServerClientManager;
import utils.ServerManager;
import utils.config.ConfigurationManager;

public class Center {
	private static final Logger logger = LoggerFactory.getLogger(Center.class);
	private static final Center instance = new Center();

	private final ServerClientManager serverManager = new ServerClientManager();

	/**
	 * 本服务信息
	 */
	private ModelProto.ServerInfo.Builder serverInfo;

	private Center() {
	}

	public static Center getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		instance.start();
	}

	public ServerClientManager getServerManager() {
		return serverManager;
	}

	public ModelProto.ServerInfo.Builder getServerInfo() {
		return serverInfo;
	}

	private void start() {
		ConfigurationManager cfgMgr = ConfigurationManager.getInstance();
		try {
			ClientProto.init();
			serverInfo = ServerManager.manageServerInfo(cfgMgr, ServerType.Center);
			List<SocketAddress> addresses = new ArrayList<>();
			String[] split = serverInfo.getIpConfig().toStringUtf8().split(":");
			addresses.add(new InetSocketAddress(split[0], Integer.parseInt(split[1])));
			new ServerService(0, CenterClient.class).start(addresses);
			logger.info("[Center Tcp Server {}:{} start success]", split[0], Integer.parseInt(split[1]));

			new CenterHttpService().start(new InetSocketAddress(split[0], cfgMgr.getInt("httpPort", 0)));
			logger.info("[Center http Server {}:{} start success]", split[0], cfgMgr.getInt("httpPort", 0));
		} catch (Exception e) {
			logger.error("[Center start error ]", e);
			System.exit(0);
		}
	}

}
