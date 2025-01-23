package room.handel;

import com.google.protobuf.Message;
import msg.MessageId;
import net.client.Sender;
import net.handler.Handler;
import proto.RoomProto;

/**
 * 请求房间列表
 */
public class ReqRoomListHandler implements Handler {

	private static final ReqRoomListHandler instance = new ReqRoomListHandler();

	public static ReqRoomListHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, int aLong, Message msg, int mapId, long sequence) {
		RoomProto.AckGetRoomList.Builder ack = RoomProto.AckGetRoomList.newBuilder();
		sender.sendMessage(MessageId.RoomMsg.ACK_ROOM_LIST.getId(), ack.build(), null, mapId, sequence);
		return true;
	}
}
