package gate;

import java.net.SocketAddress;
import java.util.List;

import gate.client.GateTcpClient;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import net.service.TCPService;

public class GateService extends TCPService {
	public GateService() {
		this(90);
	}

	public GateService(int idleTime) {
		this(new NioEventLoopGroup(), idleTime);
	}

	public GateService(EventLoopGroup eventLoopGroup, int idleTime) {
		this(eventLoopGroup, eventLoopGroup, idleTime);
	}

	public GateService(EventLoopGroup bossGroup, EventLoopGroup workGroup, int idleTime) {
		super(bossGroup, workGroup, idleTime, GateTcpClient.class);
	}

	public GateService start(List<SocketAddress> socketAddresses) {
		super.start(socketAddresses);
		return this;
	}
}
