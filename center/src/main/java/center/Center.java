package center;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import center.client.CenterClient;
import net.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.ServerClientManager;
import utils.config.ConfigurationManager;

public class Center {
	private static final Logger logger = LoggerFactory.getLogger(Center.class);
	private static Center instance = new Center();
	private Timer timer;
	private ExecutorPool executorPool;
	public ServerClientManager serverManager = new ServerClientManager();

	private Center() {
		executorPool = new ExecutorPool("Center");
		this.timer = new Timer().setRunners(executorPool);
	}

	public Future<?> execute(Runnable r) {
		return executorPool.execute(r);
	}

	public <T extends Task> CompletableFuture<T> serialExecute(T t) {
		return executorPool.serialExecute(t);
	}

	public static Center getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		instance.start();
	}

	public <T> void register(int delay, int interval, Runner<T> runner, T param) {
		this.timer.register(delay, interval, -1, runner, param);
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
