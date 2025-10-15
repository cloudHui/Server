package room.client.handle;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.RMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.RoomProto;
import room.manager.RoomManager;
import room.manager.User;
import room.manager.UserManager;

/**
 * 处理房间列表请求
 * 返回所有可用房间信息并创建用户会话
 */
@ProcessType(RMsg.REQ_ROOM_LIST_MSG)
public class ReqRoomListHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqRoomListHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		try {
			logger.info("处理房间列表请求, clientId: {}", clientId);

			// 构建响应
			RoomProto.AckGetRoomList.Builder response = RoomProto.AckGetRoomList.newBuilder();
			RoomManager.getInstance().getAllRoomTable(response);

			// 发送响应
			sender.sendMessage(clientId, RMsg.ACK_ROOM_LIST_MSG, mapId, response.build(), sequence);

			// 创建或更新用户会话
			createOrUpdateUserSession(clientId);

			logger.info("房间列表请求处理完成, clientId: {}, 房间数量: {}", clientId, response.getRoomListCount());
			return true;
		} catch (Exception e) {
			logger.error("处理房间列表请求失败, clientId: {}", clientId, e);
			return false;
		}
	}

	/**
	 * 创建或更新用户会话
	 */
	private void createOrUpdateUserSession(int clientId) {
		User existingUser = UserManager.getInstance().getUser(clientId);
		if (existingUser == null) {
			User newUser = new User(clientId);
			UserManager.getInstance().addUser(newUser);
			logger.debug("创建新用户会话, clientId: {}", clientId);
		} else {
			logger.debug("用户会话已存在, clientId: {}", clientId);
		}
	}
}