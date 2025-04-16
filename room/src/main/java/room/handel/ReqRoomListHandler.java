package room.handel;

import com.google.protobuf.Message;
import msg.MessageId;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.RoomProto;

/**
 * 请求房间列表
 */
@ProcessType(MessageId.REQ_ROOM_LIST_MSG)
public class ReqRoomListHandler implements Handler {

	@Override
	public boolean handler(Sender sender, int aLong, Message msg, int mapId, long sequence) {
		RoomProto.AckGetRoomList.Builder ack = RoomProto.AckGetRoomList.newBuilder();
		sender.sendMessage(MessageId.RoomMsg.ACK_ROOM_LIST.getId(), ack.build(), mapId, sequence);
		return true;
	}
}
