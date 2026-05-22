package hall.client.handle.server.center;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.Message;
import hall.manager.TokenManager;
import hall.manager.User;
import hall.manager.UserManager;
import msg.annotation.ProcessType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ServerProto;

/**
 * 处理玩家断线通知
 * 清理用户会话，使Token失效
 */
@ProcessType(CMsg.NOT_BREAK)
public class NotBreakHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(NotBreakHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			ServerProto.NotBreak notification = (ServerProto.NotBreak) message;
			int userId = notification.getUserId();

			logger.info("处理玩家断线通知, userId: {}", userId);

			// 清理用户会话
			User user = UserManager.getInstance().getUser(userId);
			if (user != null) {
				// 使Token失效，下次登录需要重新认证
				TokenManager.getInstance().invalidateUser(userId);
				// 移除用户会话
				UserManager.getInstance().removeUser(userId);
				logger.info("玩家断线清理完成, userId: {}", userId);
			} else {
				logger.debug("断线玩家不存在于会话中, userId: {}", userId);
			}

		} catch (Exception e) {
			logger.error("处理断线通知失败, clientId: {}", clientId, e);
		}
		return true;
	}
}
