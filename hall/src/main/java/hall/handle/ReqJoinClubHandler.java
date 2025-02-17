package hall.handle;

import com.google.protobuf.Message;
import msg.MessageId;
import net.client.Sender;
import net.handler.Handler;
import proto.HallProto;

/**
 * 加入工会请求
 */
public class ReqJoinClubHandler implements Handler {

	private static final ReqJoinClubHandler instance = new ReqJoinClubHandler();

	public static ReqJoinClubHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		HallProto.ReqJoinClub req = (HallProto.ReqJoinClub) msg;
		HallProto.AckJoinClub.Builder ack = HallProto.AckJoinClub.newBuilder();
		sender.sendMessage(clientId, MessageId.HallMsg.ACK_JOIN_CLUB.getId(), mapId, 0, ack.build(), sequence);
		return true;
	}
}
