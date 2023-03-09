package game;

import java.net.SocketAddress;
import java.util.List;

import game.client.GameClient;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import net.service.TCPService;

public class GameService extends TCPService {

	public GameService() {
		this(0);
	}

	public GameService(int idleTime) {
		this(new NioEventLoopGroup(), idleTime);
	}

	public GameService(EventLoopGroup eventLoopGroup, int idleTime) {
		this(eventLoopGroup, eventLoopGroup, idleTime);
	}

	public GameService(EventLoopGroup bossGroup, EventLoopGroup workGroup, int idleTime) {
		super(bossGroup, workGroup, idleTime, GameClient.class);
	}

	public GameService start(List<SocketAddress> socketAddresses) {
		super.start(socketAddresses);
		return this;
	}
}
