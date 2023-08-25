package room.handel;

import msg.Message;
import net.client.Sender;
import net.handler.Handler;
import proto.HallProto;
import proto.RoomProto;

/**
 * 请求房间列表
 */
public class ReqRoomListHandler implements Handler<RoomProto.ReqGetRoomList> {

	private static ReqRoomListHandler instance = new ReqRoomListHandler();

	public static ReqRoomListHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, RoomProto.ReqGetRoomList req, int mapId) {
		RoomProto.AckGetRoomList.Builder ack = RoomProto.AckGetRoomList.newBuilder();
		sender.sendMessage(Message.RoomMsg.ACK_ROOM_LIST.getId(), ack.build(), null, mapId);
		return true;
	}
}
