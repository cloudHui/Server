package hall.client.handle;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.Message;
import hall.Hall;
import hall.connect.ConnectProcessor;
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

/**
 * 处理用户登录请求
 * 负责用户认证、会话创建和房间信息同步
 */
@ProcessType(HMsg.REQ_LOGIN_MSG)
public class ReqLoginHandler implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqLoginHandler.class);

	// TODO: 等待数据库实现后替换为数据库自增主键
	private final AtomicInteger userIdGenerator = new AtomicInteger(1000);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, int mapId, long sequence) {
		try {
			HallProto.ReqLogin request = (HallProto.ReqLogin) message;
			String nickname = request.getNickName().toStringUtf8();
			String certificate = request.getCert().toStringUtf8();

			logger.info("处理登录请求, clientId: {}, nickname: {}, cert: {}",
					clientId, nickname, certificate);

			// 处理用户登录
			User user = processUserLogin(clientId, nickname, certificate);

			// 向房间服务器请求用户房间信息
			requestUserRoomInfo(user, sequence);

			return true;
		} catch (Exception e) {
			logger.error("处理登录请求失败, clientId: {}", clientId, e);
			return false;
		}
	}

	/**
	 * 处理用户登录逻辑
	 */
	private User processUserLogin(int clientId, String nickname, String certificate) {
		User user = UserManager.getInstance().getUser(certificate);

		if (user == null) {
			// 新用户注册
			int newUserId = userIdGenerator.incrementAndGet();
			user = new User(newUserId, nickname, clientId, certificate);
			UserManager.getInstance().addUser(user);
			logger.info("新用户注册, userId: {}, nickname: {}, cert: {}", newUserId, nickname, certificate);
		} else {
			// 现有用户更新会话
			user.setClientId(clientId);
			user.setNick(nickname);
			logger.info("用户重新登录, userId: {}, nickname: {}, clientId: {}", user.getUserId(), nickname, clientId);
		}

		return user;
	}

	/**
	 * 向房间服务器请求用户房间信息
	 */
	private void requestUserRoomInfo(User user, long sequence) {
		try {
			ConnectHandler roomServer = Hall.getInstance().getServerManager().getServerClient(ServerType.Room);
			if (roomServer == null) {
				logger.warn("房间服务器不可用，无法获取用户房间信息, userId: {}", user.getUserId());
				return;
			}

			ServerProto.ReqRoomTable request = ServerProto.ReqRoomTable.newBuilder()
					.setRoleId(user.getUserId())
					.build();

			HandleManager.sendMsg(SMsg.REQ_GET_TABLE_MSG, request, roomServer,
					ConnectProcessor.PARSER, (int) sequence);

			logger.debug("已向房间服务器请求用户房间信息, userId: {}", user.getUserId());
		} catch (Exception e) {
			logger.error("请求用户房间信息失败, userId: {}", user.getUserId(), e);
		}
	}
}