package lobby.client.handle.server.gate;

import java.util.List;

import com.google.protobuf.Message;
import lobby.Lobby;
import lobby.manager.User;
import lobby.manager.UserManager;
import lobby.manager.table.TableInfo;
import lobby.manager.table.TableManager;
import msg.annotation.ProcessType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;

/**
 * 玩家断线：清理会话与桌子（不强制清 DB token，便于重连）
 */
@ProcessType(CMsg.NOT_BREAK)
public class NotBreakHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(NotBreakHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, long mapId, int sequence) {
		try {
			ServerProto.NotBreak notice = (ServerProto.NotBreak) msg;
			int userId = notice.getUserId();
			logger.info("收到玩家断线通知, userId: {}", userId);
			handleUserDisconnect(userId);
			return true;
		} catch (Exception e) {
			logger.error("处理断线通知失败, clientId: {}", clientId, e);
			return false;
		}
	}

	private void handleUserDisconnect(int userId) {
		User user = UserManager.getInstance().getUser(userId);
		if (user == null) {
			logger.warn("用户不存在,无法处理断线, userId: {}", userId);
			return;
		}
		user.setOffline(true);
		List<Long> tableIds = user.getAllTables();
		for (Long tableId : tableIds) {
			TableInfo tableInfo = TableManager.getInstance().getTableById(tableId);
			if (tableInfo != null) {
				tableInfo.removeUser(user);
				if (tableInfo.getTableRoles().isEmpty()) {
					TableManager.getInstance().removeTable(tableId);
				}
			}
		}
		UserManager.getInstance().removeUser(userId);
		logger.info("断线用户已清理, userId: {}, 桌子数: {}", userId, tableIds.size());
	}
}
