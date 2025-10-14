package room.client.handle;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.RMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.RoomProto;
import room.manager.RoomManager;

/**
 * 请求房间列表
 */
@ProcessType(RMsg.REQ_ROOM_LIST_MSG)
public class ReqRoomListHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		RoomProto.AckGetRoomList.Builder ack = RoomProto.AckGetRoomList.newBuilder();
		RoomManager.getInstance().getAllRoomTable(ack);
		sender.sendMessage(clientId, RMsg.ACK_ROOM_LIST_MSG, mapId, ack.build(), sequence);
		return true;
	}
}
