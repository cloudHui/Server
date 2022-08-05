import java.net.SocketAddress;

import http.handler.HttpResponseMaker;
import http.server.HttpService;
import io.netty.channel.nio.NioEventLoopGroup;
import router.client.ResponseMaker;
import router.client.ServerDecoder;

public class RouterHttpService extends HttpService {

	private static final HttpResponseMaker MAKER = new ResponseMaker();

	RouterHttpService() {
		super(new NioEventLoopGroup(), new NioEventLoopGroup());
	}

	void start(SocketAddress socketAddress) {
		super.start(socketAddress, ServerDecoder.class, MAKER);
	}
}
