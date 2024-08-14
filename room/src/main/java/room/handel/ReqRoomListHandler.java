package room.handel;

import msg.MessageId;
import net.client.Sender;
import net.handler.Handler;
import proto.RoomProto;

/**
 * 请求房间列表
 */
public class ReqRoomListHandler implements Handler<RoomProto.ReqGetRoomList> {

	private static final ReqRoomListHandler instance = new ReqRoomListHandler();

	public static ReqRoomListHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, long aLong, RoomProto.ReqGetRoomList req, int mapId) {
		RoomProto.AckGetRoomList.Builder ack = RoomProto.AckGetRoomList.newBuilder();
		sender.sendMessage(MessageId.RoomMsg.ACK_ROOM_LIST.getId(), ack.build(), null, mapId);
		return true;
	}
}
