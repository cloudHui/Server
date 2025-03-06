package robot.web.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;

public class MyWebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private final WebSocketClientHandshaker handShaker;
    private ChannelPromise handshakeFuture;
    
    public MyWebSocketClientHandler(WebSocketClientHandshaker handShaker) {
        this.handShaker = handShaker;
    }
 
    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }
 
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }
 
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handShaker.handshake(ctx.channel());
    }
 
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("webSocket client disconnected!");
    }
 
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!handShaker.isHandshakeComplete()) {
            handShaker.finishHandshake(ch, (FullHttpResponse) msg);
            System.out.println("websocket client connected!");
            handshakeFuture.setSuccess();
            return;
        }
 
        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new Exception("Unexpected FullHttpResponse (getStatus=" + response.status() + ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }
 
        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            System.out.println("客户端收到消息: " + textFrame.text());
        } else if (frame instanceof PongWebSocketFrame) {
            System.out.println("websocket client received pong");
        } else if (frame instanceof CloseWebSocketFrame) {
            System.out.println("websocket client received closing");
            ch.close();
        }
    }
 
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) {
 
    }
 
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }
 
}