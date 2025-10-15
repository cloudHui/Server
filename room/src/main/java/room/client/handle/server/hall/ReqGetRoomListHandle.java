package room.client.handle.server.hall;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ServerProto;
import room.manager.User;
import room.manager.UserManager;

/**
 * 大厅请求房间服务获取玩家已经加入的房间列表
 */
@ProcessType(SMsg.REQ_GET_TABLE_MSG)
public class ReqGetRoomListHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		ServerProto.ReqRoomTable reqRoomTable = (ServerProto.ReqRoomTable) msg;
		ServerProto.AckRoomTable.Builder ack = ServerProto.AckRoomTable.newBuilder();
		ack.setRoleId(reqRoomTable.getRoleId());
		User user = UserManager.getInstance().getUser(reqRoomTable.getRoleId());
		if (user != null) {
			ack.addAllTables(user.getAllTables());
		}
		sender.sendMessage(clientId, SMsg.ACK_GET_TABLE_MSG, mapId, ack.build(), sequence);
		return true;
	}
}
