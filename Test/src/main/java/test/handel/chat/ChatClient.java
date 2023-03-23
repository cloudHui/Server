package test.handel.chat;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

public class ChatClient {
	private static final String HOST = "localhost";
	private static final int PORT = 8080;

	public static void main(String[] args) throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();

		try {
			Bootstrap b = new Bootstrap();
			b.group(group)
					.channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline pipeline = ch.pipeline();

							//添加一个ProtobufVarint32LengthFieldPrepender来解决TCP粘包的问题
							pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
							//添加一个ProtobufEncoder来序列化Java对象为protobuf消息
							pipeline.addLast(new ProtobufEncoder());
							//添加自定义的 handler 处理业务逻辑
//                            pipeline.addLast(new ChatClientHandler());
						}
					});

			ChannelFuture f = b.connect(HOST, PORT).sync();

			//向服务器发送消息
			Chat.ChatMessage.Builder message = Chat.ChatMessage.newBuilder()
					.setFromUser("User1")
					.setMessage("Hello, World!");
			f.channel().writeAndFlush(message.build());

			//阻塞主线程，等待Channel关闭后才退出
			f.channel().closeFuture().sync();
		} finally {
			group.shutdownGracefully();
		}
	}
}