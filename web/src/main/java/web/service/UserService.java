package web.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import msg.registor.message.HMsg;
import msg.registor.message.RMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import proto.RoomProto;
import org.springframework.stereotype.Service;

/**
 * 用户会话管理
 * 管理Web用户会话，处理登录和Token验证
 */
@Service
public class UserService {
	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	private final GateClient gateClient;
	private final ReentrantLock sessionLock = new ReentrantLock();

	/** sessionId -> UserInfo */
	private final Map<String, UserInfo> sessions = new ConcurrentHashMap<>();
	/** token -> UserInfo */
	private final Map<String, UserInfo> tokenSessions = new ConcurrentHashMap<>();
	/** userId -> sessionId */
	private final Map<Integer, String> userSessions = new ConcurrentHashMap<>();

	public UserService(GateClient gateClient) {
		this.gateClient = gateClient;
	}

	/**
	 * 用户登录
	 * 通过Gate向Hall服务器发送登录请求
	 */
	public UserInfo login(String nickname) {
		String sessionId = UUID.randomUUID().toString();
		String deviceId = "web_" + sessionId.substring(0, 8);

		logger.info("用户登录请求, nickname: {}, sessionId: {}", nickname, sessionId);

		try {
			HallProto.ReqLogin reqLogin = HallProto.ReqLogin.newBuilder()
					.setCert(ByteString.copyFromUtf8(deviceId))
					.setNickName(ByteString.copyFromUtf8(nickname))
					.setToken(ByteString.EMPTY)
					.setChannel(1)
					.build();

			CompletableFuture<Message> future = gateClient.sendAndWait(
					sessionId, HMsg.REQ_LOGIN_MSG, reqLogin, 5);

			Message response = future.get(5, TimeUnit.SECONDS);

			if (response instanceof HallProto.AckLogin) {
				HallProto.AckLogin ackLogin = (HallProto.AckLogin) response;
				String token = ackLogin.getToken().toStringUtf8();

				UserInfo userInfo = new UserInfo(
						sessionId,
						ackLogin.getUserId(),
						ackLogin.getNickName().toStringUtf8(),
						token
				);

				sessionLock.lock();
				try {
					sessions.put(sessionId, userInfo);
					tokenSessions.put(token, userInfo);
					userSessions.put(userInfo.getUserId(), sessionId);
				} finally {
					sessionLock.unlock();
				}

				logger.info("用户登录成功, userId: {}, nickname: {}, sessionId: {}",
						userInfo.getUserId(), userInfo.getNickname(), sessionId);
				return userInfo;
			} else {
				logger.error("登录响应类型错误, response: {}", response);
				return null;
			}
		} catch (Exception e) {
			logger.error("登录失败, nickname: {}, sessionId: {}", nickname, sessionId, e);
			gateClient.removeConnection(sessionId);
			return null;
		}
	}

	/**
	 * Token验证（通过Hall校验）
	 */
	public UserInfo validateToken(String token) {
		// 先查本地缓存
		UserInfo info = tokenSessions.get(token);
		if (info != null) {
			logger.info("Token验证成功(本地), userId: {}", info.getUserId());
			return info;
		}

		// 走 Hall 校验
		try {
			String sessionId = "validate_" + UUID.randomUUID().toString().substring(0, 8);
			HallProto.ReqLogin reqLogin = HallProto.ReqLogin.newBuilder()
					.setCert(ByteString.EMPTY)
					.setNickName(ByteString.EMPTY)
					.setToken(ByteString.copyFromUtf8(token))
					.setChannel(0)
					.build();

			CompletableFuture<Message> future = gateClient.sendAndWait(
					sessionId, HMsg.REQ_LOGIN_MSG, reqLogin, 5);
			Message response = future.get(5, TimeUnit.SECONDS);

			if (response instanceof HallProto.AckLogin) {
				HallProto.AckLogin ack = (HallProto.AckLogin) response;
				if (ack.getUserId() > 0) {
					String newToken = ack.getToken().toStringUtf8();
					UserInfo userInfo = new UserInfo(
							sessionId,
							ack.getUserId(),
							ack.getNickName().toStringUtf8(),
							newToken
					);

					sessionLock.lock();
					try {
						sessions.put(sessionId, userInfo);
						tokenSessions.put(newToken, userInfo);
						userSessions.put(userInfo.getUserId(), sessionId);
					} finally {
						sessionLock.unlock();
					}

					logger.info("Token验证成功(Hall), userId: {}", userInfo.getUserId());
					return userInfo;
				}
			}
		} catch (Exception e) {
			logger.error("Token验证失败(Hall), token: {}", token.substring(0, Math.min(8, token.length())), e);
		}

		logger.warn("Token验证失败");
		return null;
	}

	/**
	 * 获取会话信息
	 */
	public UserInfo getSession(String sessionId) {
		return sessions.get(sessionId);
	}

	/**
	 * 用户登出
	 */
	public void logout(String sessionId) {
		UserInfo info;
		sessionLock.lock();
		try {
			info = sessions.remove(sessionId);
			if (info != null) {
				tokenSessions.remove(info.getToken());
				userSessions.remove(info.getUserId());
			}
		} finally {
			sessionLock.unlock();
		}
		if (info != null) {
			gateClient.removeConnection(sessionId);
			logger.info("用户登出, userId: {}, sessionId: {}", info.getUserId(), sessionId);
		}
	}

	/**
	 * 获取房间列表
	 */
	public CompletableFuture<Message> getRoomList(String sessionId) {
		RoomProto.ReqRoomList request = RoomProto.ReqRoomList.newBuilder().build();
		return gateClient.sendAndWait(sessionId, RMsg.REQ_ROOM_LIST_MSG, request, 5);
	}

	/**
	 * 加入桌子
	 */
	public CompletableFuture<Message> joinTable(String sessionId, int roomId) {
		RoomProto.ReqJoinRoomTable request = RoomProto.ReqJoinRoomTable.newBuilder()
				.setRoomId(roomId)
				.build();
		return gateClient.sendAndWait(sessionId, RMsg.REQ_JOIN_ROOM_TABLE_MSG, request, 5);
	}

	/**
	 * 用户信息
	 */
	public static class UserInfo {
		private final String sessionId;
		private final int userId;
		private final String nickname;
		private final String token;

		public UserInfo(String sessionId, int userId, String nickname, String token) {
			this.sessionId = sessionId;
			this.userId = userId;
			this.nickname = nickname;
			this.token = token;
		}

		public String getSessionId() { return sessionId; }
		public int getUserId() { return userId; }
		public String getNickname() { return nickname; }
		public String getToken() { return token; }
	}
}
