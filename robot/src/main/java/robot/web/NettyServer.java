package robot.web;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class NettyServer {
	public void run(int port) {
		new Thread(() -> runServer(port)).start();
	}

	private void runServer(int port) {
		System.out.println("===============Message服务端启动===============");
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup);
			b.channel(NioServerSocketChannel.class);
			b.childHandler(new ChannelInitializer() {
				@Override
				protected void initChannel(Channel channel) {
					ChannelPipeline pipeline = channel.pipeline();
					pipeline.addLast("codec-http", new HttpServerCodec());
					pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
					pipeline.addLast("handler", new MyWebSocketServerHandler());
				}

			});

			Channel ch = b.bind(port).sync().channel();
			System.out.println("Message服务器启动成功：" + ch.toString());
			ch.closeFuture().sync();
		} catch (Exception e) {
			System.out.println("Message服务运行异常：" + e.getMessage());
			e.printStackTrace();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
			System.out.println("Message服务已关闭");
		}
	}
}