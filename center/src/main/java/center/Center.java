package center;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.config.ConfigurationManager;

public class Center {
	private static final Logger LOGGER = LoggerFactory.getLogger(Center.class);
	private static Center instance = new Center();
	private Timer timer;
	private ExecutorPool executorPool;

	private Center() {
		executorPool = new ExecutorPool("router.Router");
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
			new CenterService().start(cfgMgr.getServers().get("Center").getHostList());
			String http = cfgMgr.getProperty("http");
			String[] hosts = http.split(":");
			new CenterHttpService().start(new InetSocketAddress(hosts[0], Integer.parseInt(hosts[1])));
			LOGGER.info("[Center start success]");
		} catch (Exception e) {
			LOGGER.error("[Center start error ]", e);
			System.exit(0);
		}
	}

}
