package lobby.connect.game;

import com.google.protobuf.Message;
import lobby.manager.User;
import lobby.manager.UserManager;
import lobby.manager.table.TableInfo;
import lobby.manager.table.TableManager;
import msg.annotation.ProcessType;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;

/**
 * Game 通知 Lobby：玩家已离桌（桌子可能仍保留）。
 */
@ProcessType(SMsg.NOT_TABLE_PLAYER_LEFT_MSG)
public class NotTablePlayerLeftHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(NotTablePlayerLeftHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			ServerProto.NotTablePlayerLeft not = (ServerProto.NotTablePlayerLeft) message;
			long tableId = not.getTableId();
			int roleId = not.getRoleId();
			TableInfo tableInfo = TableManager.getInstance().getTableById(tableId);
			if (tableInfo == null) {
				logger.info("收到离桌通知但桌子不存在, tableId: {}, roleId: {}", tableId, roleId);
				return true;
			}
			User user = UserManager.getInstance().getUser(roleId);
			if (user != null) {
				tableInfo.removeUser(user);
			} else {
				// 用户已离线：按 roleId 从桌内移除归属
				for (User u : tableInfo.getTableRoles()) {
					if (u != null && u.getUserIdInt() == roleId) {
						tableInfo.removeUser(u);
						break;
					}
				}
			}
			if (tableInfo.getTableRoles().isEmpty()) {
				TableManager.getInstance().removeTable(tableId);
				logger.info("离桌后空桌，移除大厅桌子, tableId: {}, roleId: {}", tableId, roleId);
			} else {
				logger.info("大厅同步玩家离桌, tableId: {}, roleId: {}, remain: {}",
						tableId, roleId, tableInfo.getTableRoles().size());
			}
		} catch (Exception e) {
			logger.error("处理玩家离桌通知失败", e);
		}
		return true;
	}
}
