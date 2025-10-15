package hall.client.handle;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.Message;
import hall.Hall;
import hall.connect.ConnectProcessor;
import hall.manager.User;
import hall.manager.UserManager;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.HMsg;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.connect.handle.ConnectHandler;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import proto.ServerProto;
import utils.manager.HandleManager;

/**
 * 登录请求
 */
@ProcessType(HMsg.REQ_LOGIN_MSG)
public class ReqLoginHandler implements Handler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReqLoginHandler.class);

	//Todo 等待入库获取自增主键
	private final AtomicInteger uid = new AtomicInteger(1);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, int mapId, long sequence) {
		HallProto.ReqLogin req = (HallProto.ReqLogin) message;
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

			ConnectHandler serverClient = Hall.getInstance().getServerManager().getServerClient(ServerType.Room);
			HandleManager.sendMsg(SMsg.REQ_GET_TABLE_MSG, ServerProto.ReqRoomTable.newBuilder()
					.setRoleId(user.getUserId())
					.build(), serverClient, ConnectProcessor.PARSER, (int) sequence);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
}
