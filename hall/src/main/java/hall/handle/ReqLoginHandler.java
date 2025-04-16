package hall.handle;

import com.google.protobuf.Message;
import msg.MessageId;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.HallProto;

/**
 * 登录请求
 */
@ProcessType(MessageId.REQ_LOGIN_MSG)
public class ReqLoginHandler implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		HallProto.ReqLogin req = (HallProto.ReqLogin) msg;
		String avatar = req.getAvatar().toStringUtf8();
		String cert = req.getCert().toStringUtf8();
		HallProto.AckLogin.Builder ack = HallProto.AckLogin.newBuilder();
		ack.setUserId(1);
		sender.sendMessage(clientId, MessageId.HallMsg.ACK_LOGIN.getId(), mapId, 0, ack.build(), sequence);
		return true;
	}
}
