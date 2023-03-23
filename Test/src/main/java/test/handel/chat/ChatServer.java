package test.handel.chat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;

//<dependencies>
//    <dependency>
//        <groupId>io.netty</groupId>
//        <artifactId>netty-all</artifactId>
//        <version>4.1.50.Final</version>
//    </dependency>
//    <dependency>
//        <groupId>com.google.protobuf</groupId>
//        <artifactId>protobuf-java</artifactId>
//        <version>3.11.4</version>
//    </dependency>
//</dependencies>
public class ChatServer {
	private static final int PORT = 8080;

	public static void main(String[] args) throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1); //负责接受客户端连接
		EventLoopGroup workerGroup = new NioEventLoopGroup(); //负责处理客户端连接事件

		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline pipeline = ch.pipeline();

							//添加一个ProtobufVarint32FrameDecoder来解决TCP粘包的问题
							pipeline.addLast(new ProtobufVarint32FrameDecoder());
							//添加一个ProtobufDecoder来解析protobuf编码的消息
							pipeline.addLast(new ProtobufDecoder(Chat.ChatMessage.getDefaultInstance()));
							//添加自定义的 handler 处理业务逻辑
//                            pipeline.addLast(new ChatServerHandler());
						}
					})
					.option(ChannelOption.SO_BACKLOG, 128)
					.childOption(ChannelOption.SO_KEEPALIVE, true);

			ChannelFuture f = b.bind(PORT).sync();

			//阻塞主线程，等待Channel关闭后才退出
			f.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
}