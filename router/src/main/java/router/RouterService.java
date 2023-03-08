package router;

import java.net.SocketAddress;
import java.util.List;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import net.service.TCPService;
import router.client.RouterClient;

public class RouterService extends TCPService {
	public RouterService() {
		this(0);
	}

	public RouterService(int idleTime) {
		this(new NioEventLoopGroup(), idleTime);
	}

	public RouterService(EventLoopGroup eventLoopGroup, int idleTime) {
		this(eventLoopGroup, eventLoopGroup, idleTime);
	}

	public RouterService(EventLoopGroup bossGroup, EventLoopGroup workGroup, int idleTime) {
		super(bossGroup, workGroup, idleTime, RouterClient.class);
	}

	public RouterService start(List<SocketAddress> socketAddresses) {
		super.start(socketAddresses);
		return this;
	}
}
