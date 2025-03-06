package robot.webs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

/**
 * @author admin
 * @className Websocket
 * @description
 * @createDate 2025/3/5 9:36
 */
public class WebsocketServer {
	public static void main(String[] args) throws InterruptedException, IOException {
		int port = 5601; // 843 flash policy port

		MySocketServer s = null;
		try {
			s = new MySocketServer(port);
			s.start();
			System.out.println("ChatServer started on port: " + s.getPort());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		if (s != null) {
			BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				String in = sysin.readLine();
				s.broadcast(in);
				if (in.equals("exit")) {
					s.stop(1000);
					break;
				}
			}
		}
	}
}
