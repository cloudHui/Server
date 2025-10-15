package center;

import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import center.client.ResponseMaker;
import center.client.ServerDecoder;
import http.handler.HttpResponseMaker;
import http.server.HttpService;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * 中心服务器HTTP服务
 * 提供管理接口和客户端网关分配服务
 */
class CenterHttpService extends HttpService {
	private static final Logger logger = LoggerFactory.getLogger(CenterHttpService.class);
	private static final HttpResponseMaker RESPONSE_MAKER = new ResponseMaker();

	CenterHttpService() {
		super(new NioEventLoopGroup(), new NioEventLoopGroup());
		logger.debug("创建中心服务器HTTP服务");
	}

	void start(SocketAddress address) {
		try {
			super.start(address, ServerDecoder.class, RESPONSE_MAKER);
			logger.info("中心服务器HTTP服务启动成功, 地址: {}", address);
		} catch (Exception e) {
			logger.error("中心服务器HTTP服务启动失败, 地址: {}", address, e);
			throw new RuntimeException("HTTP服务启动失败", e);
		}
	}
}