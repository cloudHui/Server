package hall.connect.room;

import java.util.List;

import hall.manager.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import hall.Hall;
import hall.manager.User;
import msg.annotation.ProcessClass;
import msg.registor.enums.ServerType;
import msg.registor.message.HMsg;
import net.connect.handle.ConnectHandler;
import proto.HallProto;
import proto.ServerProto;
import utils.manager.ConnectHandle;

/**
 * 处理房间服务器返回的用户房间列表
 * 将房间信息返回给网关服务器完成登录流程
 */
@ProcessClass(ServerProto.AckRoomTable.class)
public class AckRoomTableHandler implements ConnectHandle {
	private static final Logger logger = LoggerFactory.getLogger(AckRoomTableHandler.class);

	// 最大重试次数
	private static final int MAX_RETRY_COUNT = 3;
	// 重试间隔（毫秒）
	private static final long RETRY_INTERVAL = 5000;

	@Override
	public void handle(Message message, ConnectHandler serverClient, int sequence) {
		try {
			if (message instanceof ServerProto.AckRoomTable) {
				ServerProto.AckRoomTable roomTable = (ServerProto.AckRoomTable) message;
				int roleId = roomTable.getRoleId();
				List<ServerProto.RoomTableInfo> rooms = roomTable.getTablesList();

				logger.info("收到用户房间列表, userId: {}, 房间数量: {}", roleId, rooms.size());

				// 延迟发送登录响应，确保网关服务器已准备好
				scheduleLoginResponse(roleId, rooms, sequence);
			}
		} catch (Exception e) {
			logger.error("处理房间表响应失败", e);
		}
	}

	/**
	 * 调度登录响应发送（带重试机制）
	 */
	private void scheduleLoginResponse(int roleId, List<ServerProto.RoomTableInfo> rooms, int sequence) {
		Hall.getInstance().registerTimer(0, RETRY_INTERVAL, MAX_RETRY_COUNT,
				hall -> sendLoginResponseWithRetry(roleId, rooms, sequence),
				Hall.getInstance());

		logger.debug("已调度登录响应发送, userId: {}, 最大重试次数: {}", roleId, MAX_RETRY_COUNT);
	}

	/**
	 * 发送登录响应（带重试逻辑）
	 */
	private boolean sendLoginResponseWithRetry(int roleId, List<ServerProto.RoomTableInfo> rooms, int sequence) {
		try {
			ConnectHandler gateway = Hall.getInstance().getServerManager().getServerClient(ServerType.Gate);
			if (gateway == null) {
				logger.warn("网关服务器不可用，等待重试, userId: {}", roleId);
				return false; // 返回false继续重试
			}

			User user = UserManager.getInstance().getUser(roleId);
			if (user == null) {
				logger.error("用户不存在，停止重试, userId: {}", roleId);
				return true; // 返回true停止重试
			}

			// 构建登录响应
			HallProto.AckLogin loginResponse = buildLoginResponse(user, rooms);

			// 发送响应到网关服务器
			gateway.sendMessage(roleId, HMsg.ACK_LOGIN_MSG, 0, loginResponse, sequence);

			logger.info("登录响应发送成功, userId: {}, 房间数量: {}", roleId, rooms.size());
			return true; // 返回true停止重试

		} catch (Exception e) {
			logger.error("发送登录响应失败, userId: {}", roleId, e);
			return false; // 返回false继续重试
		}
	}

	/**
	 * 构建登录响应
	 */
	private HallProto.AckLogin buildLoginResponse(User user, List<ServerProto.RoomTableInfo> rooms) {
		return HallProto.AckLogin.newBuilder()
				.setCert(ByteString.copyFromUtf8(user.getCert()))
				.setUserId(user.getUserId())
				.setNickName(ByteString.copyFromUtf8(user.getNick()))
				.addAllTables(rooms)
				.build();
	}
}