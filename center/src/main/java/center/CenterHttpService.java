package center;

import http.handler.HttpResponseMaker;
import http.server.HttpService;
import io.netty.channel.nio.NioEventLoopGroup;
import center.client.ResponseMaker;
import center.client.ServerDecoder;

import java.net.SocketAddress;

class CenterHttpService extends HttpService {
    private static final HttpResponseMaker MAKER = new ResponseMaker();

    CenterHttpService() {
        super(new NioEventLoopGroup(), new NioEventLoopGroup());
    }

    void start(SocketAddress socketAddress) {
        super.start(socketAddress, ServerDecoder.class, MAKER);
    }
}
