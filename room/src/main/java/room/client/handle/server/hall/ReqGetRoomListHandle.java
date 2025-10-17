package room.client.handle.server.hall;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;
import room.manager.user.User;
import room.manager.user.UserManager;

/**
 * 处理大厅服务器请求获取玩家已加入的房间列表
 */
@ProcessType(SMsg.REQ_GET_TABLE_MSG)
public class ReqGetRoomListHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqGetRoomListHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, int sequence) {
		try {
			ServerProto.ReqRoleRoomTable request = (ServerProto.ReqRoleRoomTable) msg;
			int roleId = request.getRoleId();

			logger.debug("处理获取房间列表请求, roleId: {}, clientId: {}", roleId, clientId);

			ServerProto.AckRoleRoomTable response = buildRoomTableResponse(roleId);
			sender.sendMessage(clientId, SMsg.ACK_GET_TABLE_MSG, mapId, response, sequence);

			logger.info("返回玩家房间列表, roleId: {}, 房间数量: {}", roleId, response.getTablesCount());
			return true;
		} catch (Exception e) {
			logger.error("处理获取房间列表请求失败, clientId: {}", clientId, e);
			return false;
		}
	}

	/**
	 * 构建房间表响应
	 */
	private ServerProto.AckRoleRoomTable buildRoomTableResponse(int roleId) {
		ServerProto.AckRoleRoomTable.Builder response = ServerProto.AckRoleRoomTable.newBuilder();
		response.setRoleId(roleId);

		User user = UserManager.getInstance().getUser(roleId);
		if (user != null) {
			response.addAllTables(user.getAllTables());
			logger.debug("找到用户房间信息, roleId: {}, 房间数: {}", roleId, user.getAllTables().size());
		} else {
			logger.warn("用户不存在,返回空房间列表, roleId: {}", roleId);
		}

		return response.build();
	}
}