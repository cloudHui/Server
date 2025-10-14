package hall.client.handle;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import hall.manager.User;
import hall.manager.UserManager;
import msg.annotation.ProcessType;
import msg.registor.message.HMsg;
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
		String cert = req.getCert().toStringUtf8();
		try {
			User user = UserManager.getInstance().getUser(cert);
			if (user == null) {
				user = new User(uid.incrementAndGet(), nick, clientId, cert);
				UserManager.getInstance().addUser(user);
			} else {
				user.setClientId(clientId);
				user.setNick(nick);
			}
			sender.sendMessage(clientId, HMsg.ACK_LOGIN_MSG, mapId,
					HallProto.AckLogin.newBuilder()
							.setCert(req.getCert())
							.setUserId(user.getUserId())
							.setNickName(ByteString.copyFromUtf8(nick))
							.build(), sequence);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
}
