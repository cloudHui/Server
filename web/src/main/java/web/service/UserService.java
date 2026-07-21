package web.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import msg.registor.message.LMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import proto.LobbyProto;

/**
 * 用户会话管理：经 Gate 访问 Lobby 登录/注册/房间
 */
@Service
public class UserService {
	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	private final GateClient gateClient;
	private final ReentrantLock sessionLock = new ReentrantLock();

	private final Map<String, UserInfo> sessions = new ConcurrentHashMap<>();
	private final Map<String, UserInfo> tokenSessions = new ConcurrentHashMap<>();
	private final Map<Integer, String> userSessions = new ConcurrentHashMap<>();

	public UserService(GateClient gateClient) {
		this.gateClient = gateClient;
	}

	public UserInfo login(String username, String password) {
		String sessionId = UUID.randomUUID().toString();
		logger.info("用户登录请求, username: {}, sessionId: {}", username, sessionId);
		try {
			LobbyProto.ReqLogin reqLogin = LobbyProto.ReqLogin.newBuilder()
					.setUsername(ByteString.copyFromUtf8(username))
					.setPassword(ByteString.copyFromUtf8(password))
					.setToken(ByteString.EMPTY)
					.build();

			CompletableFuture<Message> future = gateClient.sendAndWait(
					sessionId, LMsg.REQ_LOGIN_MSG, reqLogin, 5);
			Message response = future.get(5, TimeUnit.SECONDS);

			if (response instanceof LobbyProto.AckLogin) {
				LobbyProto.AckLogin ack = (LobbyProto.AckLogin) response;
				if (ack.getCode() != 0 || ack.getUserId() <= 0) {
					logger.warn("登录失败, code: {}", ack.getCode());
					gateClient.removeConnection(sessionId);
					return null;
				}
				String uname = ack.getUsername().toStringUtf8();
				if (uname.isEmpty()) {
					uname = username;
				}
				return storeSession(sessionId, ack.getUserId(), uname,
						ack.getNickName().toStringUtf8(), ack.getToken().toStringUtf8(),
						ack.getTablesList(), toTableInfos(ack.getTableInfosList()));
			}
			logger.error("登录响应类型错误, response: {}", response);
			return null;
		} catch (Exception e) {
			logger.error("登录失败, username: {}", username, e);
			gateClient.removeConnection(sessionId);
			return null;
		}
	}

	public UserInfo register(String username, String password, String nickname, String invite) {
		String sessionId = UUID.randomUUID().toString();
		logger.info("用户注册请求, username: {}, sessionId: {}", username, sessionId);
		try {
			LobbyProto.ReqUserRegister.Builder builder = LobbyProto.ReqUserRegister.newBuilder()
					.setUsername(ByteString.copyFromUtf8(username))
					.setPassword(ByteString.copyFromUtf8(password))
					.setNickName(ByteString.copyFromUtf8(nickname == null ? username : nickname));
			if (invite != null && !invite.isEmpty()) {
				builder.setInvite(ByteString.copyFromUtf8(invite));
			}

			CompletableFuture<Message> future = gateClient.sendAndWait(
					sessionId, LMsg.REQ_REGISTER_MSG, builder.build(), 5);
			Message response = future.get(5, TimeUnit.SECONDS);

			if (response instanceof LobbyProto.AckUserRegister) {
				LobbyProto.AckUserRegister ack = (LobbyProto.AckUserRegister) response;
				if (ack.getCode() != 0 || ack.getUserId() <= 0) {
					logger.warn("注册失败, code: {}", ack.getCode());
					gateClient.removeConnection(sessionId);
					UserInfo fail = new UserInfo(sessionId, 0, username, "", "",
							Collections.emptyList(), Collections.emptyList());
					fail.setErrorCode(ack.getCode());
					return fail;
				}
				String uname = ack.getUsername().toStringUtf8();
				if (uname.isEmpty()) {
					uname = username;
				}
				return storeSession(sessionId, ack.getUserId(), uname,
						ack.getNickName().toStringUtf8(), ack.getToken().toStringUtf8(),
						ack.getTablesList(), toTableInfos(ack.getTableInfosList()));
			}
			return null;
		} catch (Exception e) {
			logger.error("注册失败, username: {}", username, e);
			gateClient.removeConnection(sessionId);
			return null;
		}
	}

	public UserInfo validateToken(String token) {
		UserInfo info = tokenSessions.get(token);
		if (info != null) {
			return info;
		}
		try {
			String sessionId = "validate_" + UUID.randomUUID().toString().substring(0, 8);
			LobbyProto.ReqLogin reqLogin = LobbyProto.ReqLogin.newBuilder()
					.setUsername(ByteString.EMPTY)
					.setPassword(ByteString.EMPTY)
					.setToken(ByteString.copyFromUtf8(token))
					.build();

			CompletableFuture<Message> future = gateClient.sendAndWait(
					sessionId, LMsg.REQ_LOGIN_MSG, reqLogin, 5);
			Message response = future.get(5, TimeUnit.SECONDS);

			if (response instanceof LobbyProto.AckLogin) {
				LobbyProto.AckLogin ack = (LobbyProto.AckLogin) response;
				if (ack.getCode() == 0 && ack.getUserId() > 0) {
					String uname = ack.getUsername().toStringUtf8();
					String nick = ack.getNickName().toStringUtf8();
					if (uname.isEmpty()) {
						uname = nick;
					}
					return storeSession(sessionId, ack.getUserId(), uname, nick,
							ack.getToken().toStringUtf8(), ack.getTablesList(),
							toTableInfos(ack.getTableInfosList()));
				}
			}
		} catch (Exception e) {
			logger.error("Token验证失败", e);
		}
		return null;
	}

	private static List<TableInfoView> toTableInfos(List<LobbyProto.TableSeatInfo> list) {
		if (list == null || list.isEmpty()) {
			return Collections.emptyList();
		}
		List<TableInfoView> out = new ArrayList<>();
		for (LobbyProto.TableSeatInfo t : list) {
			out.add(new TableInfoView(t.getTableId(), t.getRoomId(), t.getGameType()));
		}
		return out;
	}

	private UserInfo storeSession(String sessionId, int userId, String username,
								  String nickname, String token, List<Long> tables,
								  List<TableInfoView> tableInfos) {
		List<Long> tableList = tables == null ? Collections.emptyList() : new ArrayList<>(tables);
		List<TableInfoView> infos = tableInfos == null ? Collections.emptyList() : tableInfos;
		if (infos.isEmpty() && !tableList.isEmpty()) {
			infos = new ArrayList<>();
			for (Long id : tableList) {
				infos.add(new TableInfoView(id, 0, 0));
			}
		}
		UserInfo userInfo = new UserInfo(sessionId, userId, username, nickname, token, tableList, infos);
		sessionLock.lock();
		try {
			String oldSession = userSessions.put(userId, sessionId);
			if (oldSession != null && !oldSession.equals(sessionId)) {
				sessions.remove(oldSession);
				gateClient.removeConnection(oldSession);
				logger.info("踢掉同用户旧会话, userId: {}, oldSession: {}", userId, oldSession);
			}
			sessions.put(sessionId, userInfo);
			tokenSessions.put(token, userInfo);
		} finally {
			sessionLock.unlock();
		}
		logger.info("会话建立, userId: {}, username: {}, tables: {}, sessionId: {}",
				userId, username, tableList.size(), sessionId);
		return userInfo;
	}

	public UserInfo getSession(String sessionId) {
		return sessions.get(sessionId);
	}

	public void logout(String sessionId) {
		UserInfo info;
		sessionLock.lock();
		try {
			info = sessions.remove(sessionId);
			if (info != null) {
				tokenSessions.remove(info.getToken());
				userSessions.remove(info.getUserId(), sessionId);
			}
		} finally {
			sessionLock.unlock();
		}
		if (info != null) {
			gateClient.removeConnection(sessionId);
			logger.info("用户登出, userId: {}, sessionId: {}", info.getUserId(), sessionId);
		}
	}

	public void setPushListener(BiConsumer<String, net.message.TCPMessage> listener) {
		gateClient.setPushListener(listener);
	}

	public CompletableFuture<Message> getRoomList(String sessionId) {
		LobbyProto.ReqRoomList request = LobbyProto.ReqRoomList.newBuilder().build();
		return gateClient.sendAndWait(sessionId, LMsg.REQ_ROOM_LIST_MSG, request, 5);
	}

	public CompletableFuture<Message> joinTable(String sessionId, int roomId) {
		LobbyProto.ReqJoinRoomTable request = LobbyProto.ReqJoinRoomTable.newBuilder()
				.setRoomId(roomId)
				.build();
		return gateClient.sendAndWait(sessionId, LMsg.REQ_JOIN_ROOM_TABLE_MSG, request, 5);
	}

	public static class TableInfoView {
		private final long tableId;
		private final int roomId;
		private final int gameType;

		public TableInfoView(long tableId, int roomId, int gameType) {
			this.tableId = tableId;
			this.roomId = roomId;
			this.gameType = gameType;
		}

		public long getTableId() { return tableId; }
		public int getRoomId() { return roomId; }
		public int getGameType() { return gameType; }

		public Map<String, Object> toMap() {
			Map<String, Object> m = new HashMap<>();
			m.put("tableId", tableId);
			m.put("roomId", roomId);
			m.put("gameType", gameType);
			return m;
		}
	}

	public static class UserInfo {
		private final String sessionId;
		private final int userId;
		private final String username;
		private final String nickname;
		private final String token;
		private final List<Long> tables;
		private final List<TableInfoView> tableInfos;
		private int errorCode;

		public UserInfo(String sessionId, int userId, String username, String nickname,
						String token, List<Long> tables, List<TableInfoView> tableInfos) {
			this.sessionId = sessionId;
			this.userId = userId;
			this.username = username == null ? "" : username;
			this.nickname = nickname;
			this.token = token;
			this.tables = tables == null ? Collections.emptyList() : tables;
			this.tableInfos = tableInfos == null ? Collections.emptyList() : tableInfos;
		}

		public String getSessionId() { return sessionId; }
		public int getUserId() { return userId; }
		public String getUsername() { return username; }
		public String getNickname() { return nickname; }
		public String getToken() { return token; }
		public List<Long> getTables() { return tables; }
		public List<TableInfoView> getTableInfos() { return tableInfos; }
		public boolean isAdmin() { return "admin".equals(username); }
		public int getErrorCode() { return errorCode; }
		public void setErrorCode(int errorCode) { this.errorCode = errorCode; }
	}
}
