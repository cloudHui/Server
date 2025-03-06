package robot;

import java.net.InetSocketAddress;

import io.netty.channel.nio.NioEventLoopGroup;
import msg.MessageId;
import net.connect.TCPConnect;
import net.connect.WSTCPConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import robot.connect.ConnectProcessor;
import threadtutil.thread.ExecutorPool;
import threadtutil.thread.Task;
import threadtutil.timer.Runner;
import threadtutil.timer.Timer;
import utils.ServerManager;

public class Robot {
	private final static Logger LOGGER = LoggerFactory.getLogger(Robot.class);

	private final static Robot instance = new Robot();

	private final ExecutorPool executorPool;
	private final Timer timer;

	private ServerManager serverManager;

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


	private void start() {
		serverManager = new ServerManager();
		String[] ipPort = new String[2];
		ipPort[0] = "172.20.16.119";
		ipPort[1] = "5600";
		//tcpConnect(ipPort);
		wsConnect(ipPort);
		LOGGER.info("[robot server is start!!!]");
	}

	private void tcpConnect(String[] ipPort) {
		TCPConnect tcpConnection = new TCPConnect(new NioEventLoopGroup(1),
				new InetSocketAddress(ipPort[0], Integer.parseInt(ipPort[1])),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER, ConnectProcessor.HANDLERS,
				channelHandler -> {
					TCPConnect tcpConnect = (TCPConnect) channelHandler;
					HallProto.ReqLogin.Builder ack = HallProto.ReqLogin.newBuilder();
					tcpConnect.sendMessage(MessageId.HallMsg.REQ_LOGIN.getId(), ack.build());
				}, null);
		tcpConnection.connect();
	}

	private void wsConnect(String[] ipPort) {
		WSTCPConnect wstcpConnect = new WSTCPConnect(new NioEventLoopGroup(1),
				new InetSocketAddress(ipPort[0], 5601),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER, ConnectProcessor.HANDLERS,
				channelHandler -> {
					WSTCPConnect handler = (WSTCPConnect) channelHandler;
					HallProto.ReqLogin.Builder ack = HallProto.ReqLogin.newBuilder();
					handler.sendMessage(MessageId.HallMsg.REQ_LOGIN.getId(), ack.build());
				});
		wstcpConnect.connect();
	}

	public static void main(String[] args) {
		try {
			instance.start();
		} catch (Exception e) {
			LOGGER.error("[failed for start robot server!]", e);
			System.exit(0);
		}
	}
}
