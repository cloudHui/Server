package room.client.handle;

import com.google.protobuf.Message;
import msg.RoomMessageId;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.RoomProto;

/**
 * 请求房间列表
 */
@ProcessType(RoomMessageId.REQ_ROOM_LIST_MSG)
public class ReqRoomListHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int aLong, Message msg, int mapId, long sequence) {
		RoomProto.AckGetRoomList.Builder ack = RoomProto.AckGetRoomList.newBuilder();
		sender.sendMessage(RoomMessageId.ACK_ROOM_LIST_MSG, ack.build(), mapId, sequence);
		return true;
	}
}
