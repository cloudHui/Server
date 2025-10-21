package room.client.handle.server.gate;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import room.manager.user.User;
import room.manager.user.UserManager;

/**
 * 处理玩家断线通知
 * 当玩家从网关断开时,中心服务器会发送此通知
 */
@ProcessType(CMsg.NOT_BREAK)
public class NotBreakHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(NotBreakHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, long mapId, int sequence) {
		try {
			ModelProto.NotBreak notice = (ModelProto.NotBreak) msg;
			int userId = notice.getUserId();

			logger.info("收到玩家断线通知, userId: {}, cert: {}",
					userId, notice.getCert().toStringUtf8());

			// 处理用户断线状态
			handleUserDisconnect(userId);

			return true;
		} catch (Exception e) {
			logger.error("处理断线通知失败, clientId: {}", clientId, e);
			return false;
		}
	}

	/**
	 * 处理用户断开连接
	 */
	private void handleUserDisconnect(int userId) {
		User user = UserManager.getInstance().getUser(userId);
		if (user != null) {
			// 设置用户离线状态
			user.setOffline(true);
			logger.info("设置用户离线状态, userId: {}", userId);

			// TODO: 根据业务需求处理离线逻辑
			// 例如：清理临时数据、通知游戏服务器等
		} else {
			logger.warn("用户不存在,无法设置离线状态, userId: {}", userId);
		}
	}
}