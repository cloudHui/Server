package hall.connect.room;

import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import hall.Hall;
import hall.manager.User;
import hall.manager.UserManager;
import msg.annotation.ProcessClass;
import msg.registor.enums.ServerType;
import msg.registor.message.HMsg;
import net.client.handler.ClientHandler;
import net.connect.handle.ConnectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import proto.ServerProto;
import utils.manager.ConnectHandle;

/**
 * 处理房间服务器返回的用户房间列表
 * 将房间信息返回给网关服务器完成登录流程
 */
@ProcessClass(ServerProto.AckRoleRoomTable.class)
public class AckRoomTableHandler implements ConnectHandle {
	private static final Logger logger = LoggerFactory.getLogger(AckRoomTableHandler.class);

	@Override
	public void handle(Message message, ConnectHandler handler, int sequence, int transId) {
		try {
			if (message instanceof ServerProto.AckRoleRoomTable) {
				ServerProto.AckRoleRoomTable roomTable = (ServerProto.AckRoleRoomTable) message;
				int roleId = roomTable.getRoleId();
				List<ServerProto.RoomTableInfo> rooms = roomTable.getTablesList();
				logger.debug("收到用户房间列表, userId: {}, 房间数量: {}", roleId, rooms.size());

				//发送登录响应
				returnLoginResponse(roleId, rooms, sequence);
				logger.debug("登录响应发送, userId: {}", roleId);
			}
		} catch (Exception e) {
			logger.error("处理房间表响应失败", e);
		}
	}

	/**
	 * 发送登录响应
	 */
	private void returnLoginResponse(int roleId, List<ServerProto.RoomTableInfo> rooms, int sequence) {
		try {
			User user = UserManager.getInstance().getUser(roleId);
			if (user == null) {
				logger.error("用户不存在, userId: {}", roleId);
				return; // 返回
			}

			ClientHandler gate = Hall.getInstance().serverClientManager.getServerClient(ServerType.Gate, user.getClientId());

			if (gate == null) {
				logger.error("用户 userId: {} gate:{} 不存在, ", roleId, user.getClientId());
				return; // 返回
			}
			// 发送响应到网关服务器
			gate.sendMessage(roleId, HMsg.ACK_LOGIN_MSG, 0, buildLoginResponse(user, rooms), sequence);

			logger.debug("登录响应发送成功, userId: {}, 房间数量: {}", roleId, rooms.size());

		} catch (Exception e) {
			logger.error("发送登录响应失败, userId: {}", roleId, e);
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