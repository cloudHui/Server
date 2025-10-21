// 示例：NotBreakHandle.java 优化后
package hall.client.handle.server.center;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;
import proto.ServerProto;

/**
 * 处理玩家断线通知
 * 当玩家从网关断开时,其他服务器会发送此通知
 */
@ProcessType(CMsg.NOT_BREAK)
public class NotBreakHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(NotBreakHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			ServerProto.NotBreak notification = (ServerProto.NotBreak) message;
			int userId = notification.getUserId();

			logger.info("处理玩家断线通知, userId: {}, clientId: {}", userId, clientId);

			// TODO: 实现玩家断线后的清理逻辑
			// 例如：清理用户会话、通知房间服务器等

		} catch (Exception e) {
			logger.error("处理断线通知失败, clientId: {}", clientId, e);
		}
		return true;
	}
}