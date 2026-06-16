package hall.client.handle.role;

import com.google.protobuf.Message;
import hall.Hall;
import hall.connect.ConnectProcessor;
import hall.db.entity.UserInfos;
import hall.db.service.UserService;
import hall.manager.TokenManager;
import hall.manager.User;
import hall.manager.UserManager;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.HMsg;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.connect.handle.ConnectHandler;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import proto.ServerProto;
import utils.manager.HandleManager;
import utils.other.MD5Utils;
import utils.trace.TraceContext;

@ProcessType(HMsg.REQ_LOGIN_MSG)
public class ReqLoginHandler implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqLoginHandler.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			HallProto.ReqLogin request = (HallProto.ReqLogin) message;
			String nickname = request.getNickName().toStringUtf8();
			String deviceId = request.getCert().toStringUtf8();
			String token = request.getToken().toStringUtf8();
			int loginType = request.getLoginType();
			String loginKey = request.getLoginKey().toStringUtf8();

			logger.info("处理登录请求, gateId: {}, nickname: {}, loginType: {}, hasToken: {}",
					clientId, nickname, loginType, !token.isEmpty());

			User user;
			if (loginType == 1 && !loginKey.isEmpty()) {
				user = processAccountLogin(clientId, nickname, deviceId, loginKey);
			} else {
				user = processGuestLogin(clientId, nickname, deviceId, token);
			}

			if (user == null) {
				logger.warn("登录失败, gateId: {}, loginType: {}", clientId, loginType);
				sendLoginError(sender, clientId, sequence);
				return true;
			}

			TraceContext.setUserId((int) user.getUserId());
			requestUserRoomInfo(user, sequence);

		} catch (Exception e) {
			logger.error("处理登录请求失败, gateId: {}", clientId, e);
		}
		return true;
	}

	private User processGuestLogin(int clientId, String nickname, String deviceId, String token) {
		TokenManager tokenMgr = TokenManager.getInstance();
		UserService userService = Hall.getInstance().getUserService();

		// 1. Token 登录
		if (!token.isEmpty()) {
			long userId = tokenMgr.getUserIdByToken(token);
			if (userId > 0) {
				User user = UserManager.getInstance().getUser(userId);
				if (user != null) {
					user.setGateId(clientId);
					user.setNick(nickname);
					user.setDeviceId(deviceId);
					user.updateActiveTime();
					String newToken = tokenMgr.generateToken(userId);
					user.setPendingToken(newToken);
					updateDbLoginInfo(userService, user, newToken);
					logger.info("Token登录成功(内存), userId: {}", userId);
					return user;
				}
				UserInfos dbUser = userService.queryUserInfo(userId);
				if (dbUser != null) {
					user = loadUserFromDb(dbUser, clientId, nickname, deviceId);
					String newToken = tokenMgr.generateToken(user.getUserId());
					user.setPendingToken(newToken);
					updateDbLoginInfo(userService, user, newToken);
					logger.info("Token登录成功(DB), userId: {}", user.getUserId());
					return user;
				}
			}
			logger.warn("Token无效或过期");
		}

		// 2. DeviceId 登录
		User user = UserManager.getInstance().getUser(deviceId);
		if (user != null) {
			user.setGateId(clientId);
			user.setNick(nickname);
			user.updateActiveTime();
			String newToken = tokenMgr.generateToken(user.getUserId());
			user.setPendingToken(newToken);
			updateDbLoginInfo(userService, user, newToken);
			logger.info("设备ID重连(内存), userId: {}", user.getUserId());
			return user;
		}

		UserInfos dbUser = userService.queryByDeviceId(deviceId);
		if (dbUser != null) {
			user = loadUserFromDb(dbUser, clientId, nickname, deviceId);
			String newToken = tokenMgr.generateToken(user.getUserId());
			user.setPendingToken(newToken);
			updateDbLoginInfo(userService, user, newToken);
			logger.info("设备ID重连(DB), userId: {}", user.getUserId());
			return user;
		}

		// 3. 全新游客注册
		UserInfos newUser = new UserInfos();
		newUser.setNickName(nickname);
		newUser.setDeviceId(deviceId);
		newUser.setLoginType(0);
		newUser.setLoginKey("");
		long newUserId = userService.insertUser(newUser);
		if (newUserId <= 0) {
			logger.error("数据库插入用户失败, deviceId: {}", deviceId);
			return null;
		}

		user = new User(newUserId, nickname, clientId, deviceId);
		if (!UserManager.getInstance().addUser(user)) {
			logger.error("添加用户到内存失败, userId: {}", newUserId);
			return null;
		}
		String newToken = tokenMgr.generateToken(user.getUserId());
		user.setPendingToken(newToken);
		updateDbLoginInfo(userService, user, newToken);
		logger.info("新游客注册, userId: {}, deviceId: {}", newUserId, deviceId);
		return user;
	}

	private User processAccountLogin(int clientId, String nickname, String deviceId, String password) {
		TokenManager tokenMgr = TokenManager.getInstance();
		UserService userService = Hall.getInstance().getUserService();
		String passwordMd5 = MD5Utils.MD5(password);

		// 1. 按 loginKey(MD5) 查 DB
		UserInfos dbUser = userService.queryByLoginKey(passwordMd5);
		if (dbUser != null) {
			User user = UserManager.getInstance().getUser(dbUser.getUserId());
			if (user != null) {
				user.setGateId(clientId);
				user.setNick(nickname);
				user.setDeviceId(deviceId);
				user.updateActiveTime();
			} else {
				user = loadUserFromDb(dbUser, clientId, nickname, deviceId);
			}
			String newToken = tokenMgr.generateToken(user.getUserId());
			user.setPendingToken(newToken);
			updateDbLoginInfo(userService, user, newToken);
			logger.info("账号密码登录成功, userId: {}", user.getUserId());
			return user;
		}

		// 2. 该密码无对应账号，自动注册
		UserInfos newUser = new UserInfos();
		newUser.setNickName(nickname);
		newUser.setDeviceId(deviceId);
		newUser.setLoginType(1);
		newUser.setLoginKey(passwordMd5);
		long newUserId = userService.insertUser(newUser);
		if (newUserId <= 0) {
			logger.error("数据库插入用户失败, loginKey: {}", passwordMd5);
			return null;
		}

		User user = new User((int) newUserId, nickname, clientId, deviceId);
		if (!UserManager.getInstance().addUser(user)) {
			logger.error("添加用户到内存失败, userId: {}", newUserId);
			return null;
		}
		String newToken = tokenMgr.generateToken(user.getUserId());
		user.setPendingToken(newToken);
		updateDbLoginInfo(userService, user, newToken);
		logger.info("新账号注册, userId: {}", newUserId);
		return user;
	}

	private User loadUserFromDb(UserInfos dbUser, int clientId, String nickname, String deviceId) {
		User user = new User(dbUser.getUserId(), nickname, clientId, deviceId);
		UserManager.getInstance().addUser(user);
		return user;
	}

	private void updateDbLoginInfo(UserService userService, User user, String token) {
		try {
			UserInfos update = new UserInfos();
			update.setUserId(user.getUserId());
			update.setToken(token);
			update.setDeviceId(user.getDeviceId());
			update.setLastLoginIp("");
			userService.updateLoginInfo(update);
		} catch (Exception e) {
			logger.error("更新DB登录信息失败, userId: {}", user.getUserId(), e);
		}
	}

	private void sendLoginError(Sender sender, int clientId, int sequence) {
		try {
			HallProto.AckLogin ack = HallProto.AckLogin.newBuilder()
					.setUserId(0)
					.build();
			sender.sendMessage(clientId, HMsg.ACK_LOGIN_MSG, 0, ack, sequence);
		} catch (Exception e) {
			logger.error("发送登录失败响应失败, gateId: {}", clientId, e);
		}
	}

	private void requestUserRoomInfo(User user, int sequence) {
		try {
			ConnectHandler roomServer = Hall.getInstance().getServerManager().getServerClient(ServerType.Room);
			if (roomServer == null) {
				logger.warn("房间服务器不可用, userId: {}", user.getUserId());
				return;
			}
			ServerProto.ReqRoleRoomTable request = ServerProto.ReqRoleRoomTable.newBuilder()
					.setRoleId((int) user.getUserId())
					.build();
			HandleManager.sendMsg(SMsg.REQ_GET_TABLE_MSG, request, roomServer, ConnectProcessor.PARSER, sequence, false);
		} catch (Exception e) {
			logger.error("请求用户房间信息失败, userId: {}", user.getUserId(), e);
		}
	}
}
