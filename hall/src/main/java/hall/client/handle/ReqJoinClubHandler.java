package hall.client.handle;

import com.google.protobuf.Message;
import msg.registor.message.HMsg;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.HallProto;

/**
 * 加入工会请求
 */
@ProcessType(HMsg.REQ_JOIN_CLUB_MSG)
public class ReqJoinClubHandler implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		HallProto.ReqJoinClub req = (HallProto.ReqJoinClub) msg;
		HallProto.AckJoinClub.Builder ack = HallProto.AckJoinClub.newBuilder();
		sender.sendMessage(clientId, HMsg.ACK_JOIN_CLUB_MSG, mapId, 0, ack.build(), sequence);
		return true;
	}
}
