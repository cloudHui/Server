package robot.web.client;

import java.net.URI;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

public class NettyClient {

	public void run(String strUri) {
		new Thread(() -> runClient(strUri)).start();
	}

	private void runClient(String strUri) {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			URI uri = new URI(strUri);
			String protocol = uri.getScheme();
			if (!"ws".equals(protocol)) {
				throw new IllegalArgumentException("Unsupported protocol: " + protocol);
			}

			HttpHeaders customHeaders = new DefaultHttpHeaders();
			customHeaders.add("MyHeader", "MyValue");
			// Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
			// If you change it to V00, ping is not supported and remember to change
			// HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
			final MyWebSocketClientHandler handler =
					new MyWebSocketClientHandler(
							WebSocketClientHandshakerFactory.newHandshaker(uri,
									WebSocketVersion.V13, null, false, customHeaders));
			b.group(group);
			b.channel(NioSocketChannel.class);
			b.handler(new ChannelInitializer() {
				@Override
				protected void initChannel(Channel channel) {
					ChannelPipeline pipeline = channel.pipeline();
					pipeline.addLast("http-codec", new HttpClientCodec());
					pipeline.addLast("aggregator", new HttpObjectAggregator(8192));
					pipeline.addLast("ws-handler", handler);
				}
			});
			System.out.println("===============Message客户端启动===============");
			Channel channel = b.connect(uri.getHost(), uri.getPort()).sync().channel();
			handler.handshakeFuture().sync();
			channel.closeFuture().sync();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			group.shutdownGracefully();
		}
	}
}
