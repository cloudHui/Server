package hall.handel;

import msg.Message;
import net.client.Sender;
import net.handler.Handler;
import proto.HallProto;

/**
 * 登录请求
 */
public class ReqRoomListHandler implements Handler<HallProto.ReqGetRoomList> {

	private static ReqRoomListHandler instance = new ReqRoomListHandler();

	public static ReqRoomListHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, HallProto.ReqGetRoomList req, int mapId) {
		HallProto.AckGetRoomList.Builder ack = HallProto.AckGetRoomList.newBuilder();
		sender.sendMessage(Message.HallMsg.ACK_ROOM_LIST.getId(), ack.build(), null, mapId);
		return true;
	}
}
