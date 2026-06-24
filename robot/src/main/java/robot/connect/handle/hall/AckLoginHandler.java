package robot.connect.handle.hall;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import net.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;
import utils.manager.ConnectHandle;

/**
 * 登录回复
 * 记录登录结果（userId、token）
 */
@ProcessClass(HallProto.AckLogin.class)
public class AckLoginHandler implements ConnectHandle {
	private static final Logger logger = LoggerFactory.getLogger(AckLoginHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		if (message instanceof HallProto.AckLogin) {
			HallProto.AckLogin ack = (HallProto.AckLogin) message;
			logger.info("AckLogin: userId={}, nick={}", ack.getUserId(), ack.getNickName().toStringUtf8());
		}
	}
}
