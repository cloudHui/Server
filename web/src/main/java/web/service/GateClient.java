package web.service;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.protobuf.Message;
import io.netty.channel.nio.NioEventLoopGroup;
import javax.annotation.PreDestroy;
import net.connect.TCPConnect;
import net.message.Parser;
import msg.registor.HandleTypeRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gate TCP客户端
 * 管理到Gate服务器的TCP连接，每个Web会话对应一个TCP连接
 */
public class GateClient {
	private static final Logger logger = LoggerFactory.getLogger(GateClient.class);

	private final String gateHost;
	private final int gatePort;
	private final NioEventLoopGroup eventLoopGroup;
	private final ExecutorService connectExecutor = Executors.newCachedThreadPool();
	private final Parser parser = HandleTypeRegister::parseMessage;

	/** sessionId -> TCPConnect */
	private final Map<String, TCPConnect> connections = new ConcurrentHashMap<>();

	public GateClient(String gateHost, int gatePort) {
		this.gateHost = gateHost;
		this.gatePort = gatePort;
		this.eventLoopGroup = new NioEventLoopGroup(4);
		logger.info("Gate客户端初始化完成, gate: {}:{}", gateHost, gatePort);
	}

	/**
	 * 为会话创建或获取TCP连接
	 */
	public TCPConnect getConnection(String sessionId) {
		return connections.computeIfAbsent(sessionId, this::createConnection);
	}

	/**
	 * 获取已有连接
	 */
	public TCPConnect getExistingConnection(String sessionId) {
		return connections.get(sessionId);
	}

	/**
	 * 移除会话连接并关闭底层channel
	 */
	public void removeConnection(String sessionId) {
		TCPConnect conn = connections.remove(sessionId);
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) {
				logger.debug("关闭连接异常, sessionId: {}", sessionId);
			}
		}
		logger.info("移除Gate连接, sessionId: {}", sessionId);
	}

	/**
	 * 发送消息并等待响应
	 */
	public CompletableFuture<Message> sendAndWait(String sessionId, int msgId, Message msg, int timeoutSeconds) {
		TCPConnect conn = getConnection(sessionId);
		return conn.sendMessage(msg, msgId, timeoutSeconds);
	}

	/**
	 * 发送消息（不等待响应）
	 */
	public void send(String sessionId, int msgId, Message msg) {
		TCPConnect conn = getConnection(sessionId);
		conn.sendMessage(msgId, msg);
	}

	private TCPConnect createConnection(String sessionId) {
		try {
			InetSocketAddress addr = new InetSocketAddress(gateHost, gatePort);

			TCPConnect connect = new TCPConnect(
					eventLoopGroup, addr,
					(connectHandler, tcpMessage) -> false, // 不转发，全部走回调
					parser,
					msgId -> null, // 不注册处理器，全部走CompletableFuture回调
					client -> logger.info("Gate连接建立, sessionId: {}", sessionId),
					client -> {
						logger.info("Gate连接断开, sessionId: {}", sessionId);
						connections.remove(sessionId);
					}
			);

			// 带超时的连接，防止Gate不可达时长时间阻塞
			CompletableFuture<Void> connectFuture = CompletableFuture.runAsync(connect::connect, connectExecutor);
			try {
				connectFuture.get(5, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				connections.remove(sessionId);
				throw new RuntimeException("连接Gate服务器超时", e);
			}

			logger.info("创建Gate连接成功, sessionId: {} -> {}:{}", sessionId, gateHost, gatePort);
			return connect;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			logger.error("创建Gate连接失败, sessionId: {}, gate: {}:{}", sessionId, gateHost, gatePort, e);
			throw new RuntimeException("无法连接Gate服务器", e);
		}
	}

	/** 关闭所有连接和线程池 */
	@PreDestroy
	public void shutdown() {
		for (Map.Entry<String, TCPConnect> entry : connections.entrySet()) {
			try {
				entry.getValue().close();
			} catch (Exception e) {
				logger.debug("关闭连接异常, sessionId: {}", entry.getKey());
			}
		}
		connections.clear();
		connectExecutor.shutdownNow();
		eventLoopGroup.shutdownGracefully();
		logger.info("Gate客户端已关闭");
	}
}
