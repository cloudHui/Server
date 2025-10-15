package game.client.handle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 处理网关通知的玩家断线事件
 * 负责清理玩家在游戏服务器中的状态
 */
@ProcessType(CMsg.NOT_BREAK)
public class NotBreakHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(NotBreakHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, int mapId, long sequence) {
		try {
			ModelProto.NotBreak notification = (ModelProto.NotBreak) message;
			int userId = notification.getUserId();

			logger.info("处理玩家断线通知, userId: {}, clientId: {}", userId, clientId);

			// 处理玩家断线逻辑
			processUserDisconnect(userId);

			return true;
		} catch (Exception e) {
			logger.error("处理玩家断线通知失败, clientId: {}", clientId, e);
			return false;
		}
	}

	/**
	 * 处理玩家断线逻辑
	 */
	private void processUserDisconnect(int userId) {
		// TODO: 实现玩家断线后的状态清理
		// 例如：设置玩家离线状态、从桌子中移除玩家、保存游戏进度等

		logger.debug("处理玩家断线逻辑, userId: {}", userId);

		// 示例逻辑：
		// 1. 查找玩家所在的所有桌子
		// 2. 从桌子中移除玩家
		// 3. 设置玩家离线状态
		// 4. 通知其他玩家该玩家已离线

		logger.info("玩家断线处理完成, userId: {}", userId);
	}
}