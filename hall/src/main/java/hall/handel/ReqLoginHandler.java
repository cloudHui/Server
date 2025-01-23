package hall.handel;

import com.google.protobuf.Message;
import msg.MessageId;
import net.client.Sender;
import net.handler.Handler;
import proto.HallProto;

/**
 * 登录请求
 */
public class ReqLoginHandler implements Handler {

	private static final ReqLoginHandler instance = new ReqLoginHandler();

	public static ReqLoginHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, int roleId, Message msg, int mapId, long sequence) {
		HallProto.ReqLogin req = (HallProto.ReqLogin) msg;
		String avatar = req.getAvatar().toStringUtf8();
		String cert = req.getCert().toStringUtf8();
		HallProto.AckLogin.Builder ack = HallProto.AckLogin.newBuilder();
		ack.setUserId(1);
		sender.sendMessage(ack.getUserId(), MessageId.HallMsg.ACK_LOGIN.getId(), mapId, 0, ack.build(), sequence);
		return true;
	}
}
