package hall.client.handle;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.Message;
import hall.manager.User;
import hall.manager.UserManager;
import msg.registor.message.HMsg;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.HallProto;

/**
 * 登录请求
 */
@ProcessType(HMsg.REQ_LOGIN_MSG)
public class ReqLoginHandler implements Handler {

	private final AtomicInteger uid = new AtomicInteger(1);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		HallProto.ReqLogin req = (HallProto.ReqLogin) msg;
		String nick = req.getNickName().toStringUtf8();
		HallProto.AckLogin.Builder ack = HallProto.AckLogin.newBuilder();
		ack.setUserId(uid.decrementAndGet());
		User user = UserManager.getInstance().getUser(ack.getUserId());
		if (user == null) {
			user = new User(ack.getUserId(), nick, clientId);
			UserManager.getInstance().addUser(user);
		}
		sender.sendMessage(clientId, HMsg.ACK_LOGIN_MSG, mapId, 0, ack.build(), sequence);
		return true;
	}
}
