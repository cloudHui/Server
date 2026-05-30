package hall.client.handle.role;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.Message;
import hall.Hall;
import hall.connect.ConnectProcessor;
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
import utils.trace.TraceContext;

/**
 * 处理用户登录请求
 * 支持设备ID首次登录 + Token重连
 * Token有效期14天，活跃续期
 */
@ProcessType(HMsg.REQ_LOGIN_MSG)
public class ReqLoginHandler implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqLoginHandler.class);

	// TODO: 等待数据库实现后替换为数据库自增主键
	private final AtomicInteger userIdGenerator = new AtomicInteger(1000);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			HallProto.ReqLogin request = (HallProto.ReqLogin) message;
			String nickname = request.getNickName().toStringUtf8();
			String deviceId = request.getCert().toStringUtf8();
			String token = request.getToken().toStringUtf8();

			logger.info("处理登录请求, gateId: {}, nickname: {}, deviceId: {}, hasToken: {}",
					clientId, nickname, deviceId, !token.isEmpty());

			// 处理用户登录
			User user = processUserLogin(clientId, nickname, deviceId, token);

			if (user == null) {
				logger.warn("登录失败, gateId: {}", clientId);
				return true;
			}

			// 设置链路追踪用户ID
			TraceContext.setUserId(user.getUserId());

			// 向房间服务器请求用户房间信息
			requestUserRoomInfo(user, sequence);

		} catch (Exception e) {
			logger.error("处理登录请求失败, gateId: {}", clientId, e);
		}
		return true;
	}

	/**
	 * 处理用户登录逻辑
	 */
	private User processUserLogin(int clientId, String nickname, String deviceId, String token) {
		TokenManager tokenMgr = TokenManager.getInstance();
		User user = null;

		// 1. 有Token -> 通过Token查找用户
		if (!token.isEmpty()) {
			int userId = tokenMgr.getUserIdByToken(token);
			if (userId > 0) {
				user = UserManager.getInstance().getUser(userId);
				if (user != null) {
					// 设备切换：踢掉旧设备
					String oldDeviceId = user.getDeviceId();
					if (oldDeviceId != null && !oldDeviceId.equals(deviceId)) {
						logger.info("设备切换, userId: {}, oldDevice: {}, newDevice: {}",
								userId, oldDeviceId, deviceId);
					}
					// 更新会话
					user.setGateId(clientId);
					user.setNick(nickname);
					user.setDeviceId(deviceId);
					user.updateActiveTime();

					// 生成新Token
					String newToken = tokenMgr.generateToken(userId);
					user.setPendingToken(newToken);
					logger.info("Token登录成功, userId: {}", userId);
					return user;
				}
			}
			logger.warn("Token无效或过期, token: {}", token);
		}

		// 2. 无Token -> 通过设备ID查找用户
		user = UserManager.getInstance().getUser(deviceId);
		if (user != null) {
			// 设备ID已存在 -> 同设备重连
			user.setGateId(clientId);
			user.setNick(nickname);
			user.updateActiveTime();
			String newToken = tokenMgr.generateToken(user.getUserId());
			user.setPendingToken(newToken);
			logger.info("设备ID重连, userId: {}", user.getUserId());
			return user;
		}

		// 3. 全新用户注册
		int newUserId = userIdGenerator.incrementAndGet();
		user = new User(newUserId, nickname, clientId, deviceId);
		UserManager.getInstance().addUser(user);
		String newToken = tokenMgr.generateToken(newUserId);
		user.setPendingToken(newToken);
		logger.info("新用户注册, userId: {}, nickname: {}, deviceId: {}",
				newUserId, nickname, deviceId);
		return user;
	}

	/**
	 * 向房间服务器请求用户房间信息
	 */
	private void requestUserRoomInfo(User user, int sequence) {
		try {
			ConnectHandler roomServer = Hall.getInstance().getServerManager().getServerClient(ServerType.Room);
			if (roomServer == null) {
				logger.warn("房间服务器不可用,无法获取用户房间信息, userId: {}", user.getUserId());
				return;
			}

			ServerProto.ReqRoleRoomTable request = ServerProto.ReqRoleRoomTable.newBuilder()
					.setRoleId(user.getUserId())
					.build();

			HandleManager.sendMsg(SMsg.REQ_GET_TABLE_MSG, request, roomServer, ConnectProcessor.PARSER, sequence, false);

			logger.debug("已向房间服务器请求用户房间信息, userId: {}", user.getUserId());
		} catch (Exception e) {
			logger.error("请求用户房间信息失败, userId: {}", user.getUserId(), e);
		}
	}
}