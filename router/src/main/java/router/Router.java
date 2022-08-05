package router;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadtutil.thread.ExecutorPool;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.config.ConfigurationManager;

public class Router {
	private static final Logger LOGGER = LoggerFactory.getLogger(Router.class);
	private static Router instance = new Router();
	private Timer timer;
	private ExecutorPool executorPool;

	private Router() {
		executorPool = new ExecutorPool("router.Router");
		this.timer = new Timer().setRunners(executorPool);
	}

	public Future<?> execute(Runnable r) {
		return executorPool.execute(r);
	}

	public static Router getInstance() {
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
		new RouterHttpService().start(cfgMgr.getServers().get("router.Router").getHostList().get(0));
	}

}