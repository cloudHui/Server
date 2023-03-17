package gate;

import java.net.SocketAddress;
import java.util.List;

import gate.client.GateWsClient;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import net.service.WSService;

public class GateWsService extends WSService {

	public GateWsService() {
		this(0);
	}

	public GateWsService(int idleTime) {
		this(new NioEventLoopGroup(), idleTime);
	}

	public GateWsService(EventLoopGroup eventLoopGroup, int idleTime) {
		super(eventLoopGroup, "webSocket", idleTime, GateWsClient.class);
	}


	public GateWsService start(List<SocketAddress> socketAddresses) {
		super.start(socketAddresses);
		return this;
	}
}
