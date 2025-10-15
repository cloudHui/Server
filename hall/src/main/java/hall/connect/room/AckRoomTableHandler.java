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
import net.connect.handle.ConnectHandler;
import proto.HallProto;
import proto.ServerProto;
import utils.manager.ConnectHandle;

/**
 * 获取玩家房间列表回复
 */
@ProcessClass(ServerProto.AckRoomTable.class)
public class AckRoomTableHandler implements ConnectHandle {

	@Override
	public void handle(Message message, ConnectHandler serverClient, int sequence) {
		if (message instanceof ServerProto.AckRoomTable) {
			ServerProto.AckRoomTable roomTable = (ServerProto.AckRoomTable) message;
			LOGGER.error("AckRoomTable:{}", roomTable.toString());
			//加入五秒重试机制
			Hall.getInstance().registerTimer(0, 5000, 1, hall ->
							sendLoginGetRoomBackRetry(roomTable.getRoleId(), roomTable.getTablesList(), sequence),
					Hall.getInstance());
		}
	}

	/**
	 * 发送登陆时获取房间列表回复
	 */
	private boolean sendLoginGetRoomBackRetry(int roleId, List<ServerProto.RoomTableInfo> rooms, int sequence) {
		ConnectHandler client = Hall.getInstance().getServerManager().getServerClient(ServerType.Gate);
		if (client == null) {
			return true;
		}

		User user = UserManager.getInstance().getUser(roleId);
		if (user == null) {
			return true;
		}
		client.sendMessage(roleId, HMsg.ACK_LOGIN_MSG, 0,
				HallProto.AckLogin.newBuilder()
						.setCert(ByteString.copyFromUtf8(user.getCert()))
						.setUserId(roleId)
						.setNickName(ByteString.copyFromUtf8(user.getNick()))
						.addAllTables(rooms)
						.build(), sequence);
		return false;
	}
}
