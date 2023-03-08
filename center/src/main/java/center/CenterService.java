package center;

import java.net.SocketAddress;
import java.util.List;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import net.service.TCPService;
import center.client.CenterClient;

public class CenterService extends TCPService {
	public CenterService() {
		this(0);
	}

	public CenterService(int idleTime) {
		this(new NioEventLoopGroup(), idleTime);
	}

	public CenterService(EventLoopGroup eventLoopGroup, int idleTime) {
		this(eventLoopGroup, eventLoopGroup, idleTime);
	}

	public CenterService(EventLoopGroup bossGroup, EventLoopGroup workGroup, int idleTime) {
		super(bossGroup, workGroup, idleTime, CenterClient.class);
	}

	public CenterService start(List<SocketAddress> socketAddresses) {
		super.start(socketAddresses);
		return this;
	}
}
