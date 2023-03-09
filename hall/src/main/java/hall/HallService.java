package hall;

import java.net.SocketAddress;
import java.util.List;

import hall.client.HallClient;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import net.service.TCPService;

public class HallService extends TCPService {

	public HallService() {
		this(0);
	}

	public HallService(int idleTime) {
		this(new NioEventLoopGroup(), idleTime);
	}

	public HallService(EventLoopGroup eventLoopGroup, int idleTime) {
		this(eventLoopGroup, eventLoopGroup, idleTime);
	}

	public HallService(EventLoopGroup bossGroup, EventLoopGroup workGroup, int idleTime) {
		super(bossGroup, workGroup, idleTime, HallClient.class);
	}

	public HallService start(List<SocketAddress> socketAddresses) {
		super.start(socketAddresses);
		return this;
	}
}
