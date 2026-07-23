package web.service;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import com.google.protobuf.Message;
import io.netty.channel.nio.NioEventLoopGroup;
import javax.annotation.PreDestroy;
import msg.registor.HandleTypeRegister;
import msg.registor.message.CMsg;
import msg.registor.message.GMsg;
import net.connect.TCPConnect;
import net.message.Parser;
import net.message.TCPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;

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

	/**
	 * Gate 的 roleId 只保存在 TCP 连接对象里，连接断开后即失效。
	 * 单独记录认证状态，便于 UserService 在自动重连后补发 token 登录。
	 */
	private final Set<String> authenticatedSessions = ConcurrentHashMap.newKeySet();

	/** 游戏推送监听 (sessionId, tcpMessage) */
	private volatile BiConsumer<String, TCPMessage> pushListener;

	public GateClient(String gateHost, int gatePort) {
		this.gateHost = gateHost;
		this.gatePort = gatePort;
		this.eventLoopGroup = new NioEventLoopGroup(4);
		logger.info("Gate客户端初始化完成, gate: {}:{}", gateHost, gatePort);
	}

	public void setPushListener(BiConsumer<String, TCPMessage> pushListener) {
		this.pushListener = pushListener;
	}

	public TCPConnect getConnection(String sessionId) {
		TCPConnect existing = connections.get(sessionId);
		if (existing != null && existing.isActive()) {
			return existing;
		}
		if (existing != null) {
			connections.remove(sessionId, existing);
			try { existing.close(); } catch (Exception ignored) { }
		}
		return connections.computeIfAbsent(sessionId, this::createConnection);
	}

	public TCPConnect getExistingConnection(String sessionId) {
		return connections.get(sessionId);
	}

	public void removeConnection(String sessionId) {
		authenticatedSessions.remove(sessionId);
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

	public CompletableFuture<Message> sendAndWait(String sessionId, int msgId, Message msg, int timeoutSeconds) {
		TCPConnect conn = getConnection(sessionId);
		return conn.sendMessage(msg, msgId, timeoutSeconds);
	}

	public void send(String sessionId, int msgId, Message msg) {
		TCPConnect conn = getConnection(sessionId);
		conn.sendMessage(msgId, msg);
	}

	public boolean isAuthenticated(String sessionId) {
		TCPConnect connection = connections.get(sessionId);
		if (connection == null || !connection.isActive()) {
			authenticatedSessions.remove(sessionId);
			return false;
		}
		return authenticatedSessions.contains(sessionId);
	}

	public void markAuthenticated(String sessionId) {
		authenticatedSessions.add(sessionId);
	}

	private TCPConnect createConnection(String sessionId) {
		try {
			InetSocketAddress addr = new InetSocketAddress(gateHost, gatePort);
			java.util.concurrent.CountDownLatch activeLatch = new java.util.concurrent.CountDownLatch(1);

			TCPConnect connect = new TCPConnect(
					eventLoopGroup, addr,
					(connectHandler, tcpMessage) -> handleIncoming(sessionId, tcpMessage),
					parser,
					msgId -> null,
					client -> {
						activeLatch.countDown();
						logger.info("Gate连接建立, sessionId: {}", sessionId);
					},
					client -> {
						logger.info("Gate连接断开, sessionId: {}", sessionId);
						authenticatedSessions.remove(sessionId);
						connections.remove(sessionId);
					}
			);
			// Gate 服务端 90 秒无通信会断开客户端连接；发送心跳保持大厅会话。
			connect.setIdleRunner(client -> client.sendMessage(CMsg.HEART,
					ServerProto.ReqHeart.newBuilder().build()));

			CompletableFuture<Void> connectFuture = CompletableFuture.runAsync(connect::connect, connectExecutor);
			try {
				connectFuture.get(5, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				connections.remove(sessionId);
				throw new RuntimeException("连接Gate服务器超时", e);
			}

			// connect().sync() 返回时 channelActive 可能尚未设置 completerGroup，需等到激活回调
			if (!activeLatch.await(3, TimeUnit.SECONDS)) {
				connections.remove(sessionId);
				try {
					connect.close();
				} catch (Exception ignored) {
				}
				throw new RuntimeException("Gate通道激活超时");
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

	/**
	 * 推送类消息转给 WebSocket；请求响应（非推送）返回 false 走 CompletableFuture
	 */
	private boolean handleIncoming(String sessionId, TCPMessage tcpMessage) {
		int msgId = tcpMessage.getMessageId();
		if (msgId == CMsg.HEART_ACK) {
			return true;
		}
		// 入桌回复带 sequence，必须走 sendAndWait；仅无 sequence 的座位刷新当推送
		if (msgId == GMsg.ACK_ENTER_TABLE_MSG && tcpMessage.getSequence() != 0) {
			return false;
		}
		if (!isPushMessage(msgId)) {
			return false;
		}
		BiConsumer<String, TCPMessage> listener = pushListener;
		if (listener != null) {
			try {
				listener.accept(sessionId, tcpMessage);
			} catch (Exception e) {
				logger.error("推送转发失败, sessionId: {}, msgId: 0x{}",
						sessionId, Integer.toHexString(tcpMessage.getMessageId()), e);
			}
		}
		return true;
	}

	private static boolean isPushMessage(int msgId) {
		return msgId == GMsg.NOT_CARD
				|| msgId == GMsg.NOT_OP
				|| msgId == GMsg.ACK_OP
				|| msgId == GMsg.NOT_STATE
				|| msgId == GMsg.NOT_RESULT
				|| msgId == GMsg.MJ_TILE_NOT
				|| msgId == GMsg.NOT_ROUND_RESULT
				|| msgId == GMsg.NOT_GAME_RESULT
				|| msgId == GMsg.NOT_TABLE_STATE
				|| msgId == GMsg.ACK_ENTER_TABLE_MSG;
	}

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
		authenticatedSessions.clear();
		connectExecutor.shutdownNow();
		eventLoopGroup.shutdownGracefully();
		logger.info("Gate客户端已关闭");
	}
}
