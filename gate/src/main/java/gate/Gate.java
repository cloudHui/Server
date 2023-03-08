package gate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import http.client.HttpClientPool;
import msg.http.req.RegisterGateInfoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.config.ConfigurationManager;
import utils.config.ServerConfiguration;
import utils.utils.IpUtil;

public class Gate {
	private final static Logger LOGGER = LoggerFactory.getLogger(Gate.class);

	public final static Gate instance = new Gate();

	private final ExecutorPool executorPool;
	private final Timer timer;

	private String port;
	private String ip;
	private String innerIp;
	private String router;

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
		ServerConfiguration configuration = cfgMgr.getServers().get("gate");
		if (null == configuration || !configuration.hasHostString()) {
			LOGGER.error("ERROR! failed for can not find server config");
			return;
		}

		port = cfgMgr.getProperty("localPort");

		ip = IpUtil.getOutIp();

		innerIp = IpUtil.getLocalIP();

		router = cfgMgr.getProperty("gate");

		new GateService(90).start(configuration.getHostList());

		LOGGER.info("[START] gate server is start!!!");
	}


	public static void main(String[] args) {
		try {
			instance.start();
			instance.sendRegisterGateInfoToRouter();
		} catch (Exception e) {
			LOGGER.error("failed for start gate server!", e);
		}
	}

	/**
	 * 发送 gate ip 端口到 路由服务
	 */
	private void sendRegisterGateInfoToRouter() {
		RegisterGateInfoRequest request = new RegisterGateInfoRequest();
		request.getInnerIpPort().add(ip + ":" + port);
		request.getIpPort().add(innerIp + ":" + port);
		LOGGER.error(new HttpClientPool("utf-8").init(1).sendPost(router, request.toString()));
	}
}
