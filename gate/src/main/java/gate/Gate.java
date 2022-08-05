package gate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.config.ConfigurationManager;
import utils.config.ServerConfiguration;

public class Gate {
	private final static Logger LOGGER = LoggerFactory.getLogger(Gate.class);

	public final static Gate INSTANCE = new Gate();

	private final ExecutorPool executorPool;
	private final Timer timer;

	private Gate() {
		executorPool = new ExecutorPool("Gate");
		timer = new Timer().setRunners(executorPool);
	}

	public <T> void registerTimer(int delay, int interval, int count, Runner<T> runner, T param) {
		timer.register(delay, interval, count, runner, param);
	}

	public Future<?> execute(Runnable r) {
		return executorPool.execute(r);
	}

	public <T extends Task> CompletableFuture<T> serialExecute(T t) {
		return executorPool.serialExecute(t);
	}


	private void start() {

		ConfigurationManager cfgMgr = ConfigurationManager.INSTANCE().load();
		ServerConfiguration serverConfiguration = cfgMgr.getServers().get("gate");
		if (null == serverConfiguration || !serverConfiguration.hasHostString()) {
			LOGGER.error("ERROR! failed for can not find server config");
			return;
		}


		new GateService(90).start(serverConfiguration.getHostList());

		LOGGER.info("[START] gate server is start!!!");
	}


	public static void main(String[] args) {
		try {
			INSTANCE.start();
		} catch (Exception e) {
			LOGGER.error("failed for start gate server!", e);
		}
	}

}
