package test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import msg.Message;
import net.connect.TCPConnect;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
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
	private final static Logger logger = LoggerFactory.getLogger(Test.class);

	private final static Test instance = new Test();

	private final ExecutorPool executorPool;
	private final Timer timer;

	private String gate;

	private TCPConnect gateConnect;

	private ServerManager serverManager;

	public String getGate() {
		return gate;
	}

	public void setGate(String gate) {
		this.gate = gate;
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


	private void start() throws Exception {

		ConfigurationManager cfgMgr = ConfigurationManager.INSTANCE().load();

		setGate(cfgMgr.getProperty("gate"));

		logger.info("[START] test server is start!!!");

		test();
	}

	/**
	 * 测试
	 */
	private void test() throws Exception {
//		testTcpConnect();
//		sendLogin();
		webSocketServer();
		testWSTcpConnect();
	}

	/**
	 * 链接测试
	 */
	private void testTcpConnect() {
		String center = getGate();

		ServerManager serverManager = new ServerManager();
		String[] ipPort = center.split(":");

		gateConnect = serverManager.connect(new InetSocketAddress(ipPort[0], Integer.parseInt(ipPort[1])),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS);
	}

	/**
	 * 链接测试
	 */
	private void testWSTcpConnect() throws Exception {
		String center = getGate();
		String[] ipPort = center.split(":");
		URI uri = new URI("ws://" + ipPort[0] + ":" + Integer.parseInt(ipPort[1]) + "/webSocket");
		//这样的 后面的 路径不是必须的 /webSocke
		uri = new URI("ws://" + ipPort[0] + ":" + 80 + "/webSocket");
		WebSocketClient client = new WebSocketClient(uri) {
			@Override
			public void onOpen(ServerHandshake serverHandshake) {
				send("send");
//				int length = msg.getMessage() == null ? 0 : msg.getMessage().length;
//				ByteBuf buf = Unpooled.buffer(length + 40);
//				buf.writeInt(msg.getVersion());
//				buf.writeInt(msg.getMessageId());
//				buf.writeInt(length);
//				buf.writeInt(msg.getSequence());
//				if (length > 0) {
//					buf.writeBytes(msg.getMessage());
//				}
//				ByteBuffer byteBuffer = new ByteBuffer();
//				send(buf);
			}

			@Override
			public void onMessage(String s) {
				System.out.println(s);
			}

			@Override
			public void onClose(int i, String s, boolean b) {

			}

			@Override
			public void onError(Exception e) {
			}
		};
		client.connect();
	}

	public void webSocketServer() {
		WebSocketServer server = new WebSocketServer() {
			@Override
			public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
			}

			@Override
			public void onClose(WebSocket webSocket, int i, String s, boolean b) {

			}

			@Override
			public void onMessage(WebSocket webSocket, String s) {
				System.out.println(s);
				webSocket.send("back");
			}

			@Override
			public void onError(WebSocket webSocket, Exception e) {

			}

			@Override
			public void onStart() {

			}
		};
		server.start();
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
		} catch (Exception e) {
			logger.error("failed for start test server!", e);
		}
	}
}
