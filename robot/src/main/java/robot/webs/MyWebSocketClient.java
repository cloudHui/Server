package robot.webs;

import java.net.URI;
import java.net.URISyntaxException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class MyWebSocketClient extends WebSocketClient {

	public MyWebSocketClient(URI serverUri) {
		super(serverUri);
	}

	public static void main(String[] args) throws URISyntaxException {
		MyWebSocketClient client = new MyWebSocketClient(new URI("ws://172.20.16.119:5601/ws"));
		client.connect();
	}

	@Override
	public void onOpen(ServerHandshake handShakeData) {
		System.out.println("Connected" + handShakeData.getHttpStatusMessage());
		this.send("Hello, WebSocket!"); // 发送消息到服务器
	}

	@Override
	public void onMessage(String message) {
		System.out.println("Received: " + message);
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		System.out.println("Disconnected");
	}

	@Override
	public void onError(Exception ex) {
		ex.printStackTrace();
	}
}