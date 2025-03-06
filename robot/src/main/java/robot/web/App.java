package robot.web;

import robot.web.client.NettyClient;

/**
 * @author admin
 * @className App
 * @description
 * @createDate 2025/3/5 10:56
 */
public class App {
	public static void main(String[] args) {
		NettyServer server = new NettyServer();
		server.run(9502);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		NettyClient client = new NettyClient();
		client.run("ws://localhost:" + 9502);
	}
}
