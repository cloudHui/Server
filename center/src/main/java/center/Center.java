package center;

import center.client.CenterClient;
import net.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ServerClientManager;
import utils.config.ConfigurationManager;

public class Center {
	private static final Logger logger = LoggerFactory.getLogger(Center.class);
	private static Center instance = new Center();
	public ServerClientManager serverManager = new ServerClientManager();

	private Center() {
	}

	public static Center getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		instance.start();
	}

	private void start() {
		ConfigurationManager cfgMgr = ConfigurationManager.INSTANCE().load();
		try {
			new ServerService(0, CenterClient.class).start(cfgMgr.getServers().get("center").getHostList());

			new CenterHttpService().start(cfgMgr.getServers().get("http").getHostList().get(0));
			logger.info("[Center Tcp Server start success]");
		} catch (Exception e) {
			logger.error("[Center start error ]", e);
			System.exit(0);
		}
	}

}
