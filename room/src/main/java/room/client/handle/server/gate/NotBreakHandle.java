package room.client.handle.server.gate;

import java.util.List;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;
import room.manager.table.TableInfo;
import room.manager.table.TableManager;
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
			ServerProto.NotBreak notice = (ServerProto.NotBreak) msg;
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
	 * 1. 标记用户离线
	 * 2. 从所有桌子移除用户
	 * 3. 清理空桌子
	 * 4. 从UserManager移除用户
	 */
	private void handleUserDisconnect(int userId) {
		User user = UserManager.getInstance().getUser(userId);
		if (user == null) {
			logger.warn("用户不存在,无法处理断线, userId: {}", userId);
			return;
		}

		// 标记离线
		user.setOffline(true);
		logger.info("设置用户离线状态, userId: {}", userId);

		// 从所有桌子移除用户
		List<Long> tableIds = user.getAllTables();
		for (Long tableId : tableIds) {
			TableInfo tableInfo = TableManager.getInstance().getTableById(tableId);
			if (tableInfo != null) {
				tableInfo.removeUser(user);
				// 如果桌子空了, 清理桌子
				if (tableInfo.getTableRoles().isEmpty()) {
					TableManager.getInstance().removeTable(tableId);
					logger.info("桌子已空, 清理桌子, tableId: {}", tableId);
				}
			}
		}

		// 从UserManager移除
		UserManager.getInstance().removeUser(userId);
		logger.info("断线用户已清理, userId: {}, 桌子数: {}", userId, tableIds.size());
	}
}