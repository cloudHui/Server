package center.client.handle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 处理网关服务器通知的玩家断线事件
 * 清理客户端连接映射
 */
@ProcessType(CMsg.NOT_BREAK)
public class NotBreakHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(NotBreakHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, int mapId, long sequence) {
		try {
			ModelProto.NotBreak notification = (ModelProto.NotBreak) message;
			String clientIp = notification.getCert().toStringUtf8();

			logger.info("处理玩家断线通知, clientIp: {}, userId: {}", clientIp, notification.getUserId());

			// 清理客户端连接映射
			NotClientLinkHandle.clientDisconnect(clientIp);

			return true;
		} catch (Exception e) {
			logger.error("处理断线通知失败, clientId: {}", clientId, e);
			return false;
		}
	}
}