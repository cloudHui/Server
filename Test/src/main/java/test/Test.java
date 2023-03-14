package test;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import msg.Message;
import net.connect.TCPConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import test.connect.ConnectProcessor;
import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.ServerManager;
import utils.config.ConfigurationManager;

public class Test {
	private final static Logger LOGGER = LoggerFactory.getLogger(Test.class);

	private final static Test instance = new Test();

	private final ExecutorPool executorPool;
	private final Timer timer;

	private String center;

	private TCPConnect gateConnect;

	private ServerManager serverManager;

	public String getCenter() {
		return center;
	}

	public void setCenter(String center) {
		this.center = center;
	}

	public static Test getInstance() {
		return instance;
	}


	private Test() {
		executorPool = new ExecutorPool("Test");
		timer = new Timer().setRunners(executorPool);
	}

	public <T> void registerTimer(int delay, int interval, int count, Runner<T> runner, T param) {
		timer.register(delay, interval, count, runner, param);
	}

	public <T> void registerSerialTimer(int groupId, int delay, int interval, int count, Runner<T> runner, T param) {
		timer.registerSerial(groupId, delay, interval, count, runner, param);
	}

	public Future<?> execute(Runnable r) {
		return executorPool.execute(r);
	}

	public <T extends Task> CompletableFuture<T> serialExecute(T t) {
		return executorPool.serialExecute(t);
	}


	private void start() {

		ConfigurationManager cfgMgr = ConfigurationManager.INSTANCE().load();

		setCenter(cfgMgr.getProperty("gate"));

		LOGGER.info("[START] test server is start!!!");

		test();
	}

	/**
	 * 测试
	 */
	private void test() {
		testConnect();
		sendLogin();
	}

	/**
	 * 链接测试
	 */
	private void testConnect() {
		String center = getCenter();

		ServerManager serverManager = new ServerManager();
		String[] ipPort = center.split(":");

		gateConnect = serverManager.connect(new InetSocketAddress(ipPort[0], Integer.parseInt(ipPort[1])),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS);
	}

	/**
	 * 发送登录
	 */
	private void sendLogin() {
		HallProto.ReqLogin.Builder req = HallProto.ReqLogin.newBuilder();
		req.setSex(1);
		gateConnect.sendMessage(Message.HallMsg.REQ_LOGIN.getId(), req.build(), null);
	}


	public static void main(String[] args) {
		try {
			instance.start();
//			System.out.println(MessageHandel.HallMsg.REQ_LOGIN.getId() & MessageHandel.GAME_TYPE);
		} catch (Exception e) {
			LOGGER.error("failed for start test server!", e);
		}
	}
}
