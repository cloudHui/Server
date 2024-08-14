package robot;

import msg.MessageId;
import msg.ServerType;
import net.connect.TCPConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import robot.connect.ConnectProcessor;
import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.ServerClientManager;
import utils.ServerManager;

public class Robot {
	private final static Logger LOGGER = LoggerFactory.getLogger(Robot.class);

	private final static Robot instance = new Robot();

	private final ExecutorPool executorPool;
	private final Timer timer;


	public ServerClientManager serverClientManager = new ServerClientManager();


	private ServerManager serverManager;


	public ServerManager getServerManager() {
		return serverManager;
	}

	public void setServerManager(ServerManager serverManager) {
		this.serverManager = serverManager;
	}

	public static Robot getInstance() {
		return instance;
	}


	private Robot() {
		executorPool = new ExecutorPool("Game");
		timer = new Timer().setRunners(executorPool);
	}

	public <T> void registerTimer(int delay, int interval, int count, Runner<T> runner, T param) {
		timer.register(delay, interval, count, runner, param);
	}

	public void execute(Runnable r) {
		executorPool.execute(r);
	}

	public void serialExecute(Task t) {
		executorPool.serialExecute(t);
	}


	private void start() throws Exception {
		serverManager = new ServerManager();
		String[] ipPort = new String[2];
		ipPort[0] = "127.0.0.1";
		ipPort[1] = "5600";
		TCPConnect connect = serverManager.connectSever(ipPort, ConnectProcessor.TRANSFER, ConnectProcessor.PARSER, ConnectProcessor.HANDLERS,
				0);
		serverManager.addServerClient(ServerType.Gate, connect, (int) connect.getServerId());
		checkConnect();
		LOGGER.info("[START] robot server is start!!!");
	}

	/**
	 * 检测是否链接上 gate
	 */
	private void checkConnect() {
		registerTimer(3000, 1000, -1, gate -> {
			TCPConnect serverClient = serverManager.getServerClient(ServerType.Gate);
			if (serverClient != null) {
				HallProto.ReqLogin.Builder ack = HallProto.ReqLogin.newBuilder();
				serverClient.sendMessage(MessageId.HallMsg.REQ_LOGIN.getId(), ack.build(), null);
				return true;
			}
			return false;
		}, this);
	}

	public static void main(String[] args) {
		try {
			instance.start();
		} catch (Exception e) {
			LOGGER.error("failed for start robot server!", e);
			System.exit(0);
		}
	}
}
