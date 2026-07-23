package game.client.handle.server;

import com.google.protobuf.Message;
import game.Game;
import game.manager.table.Table;
import game.manager.table.TableUser;
import msg.annotation.ProcessType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;
import utils.trace.TraceContext;

/**
 * 处理网关通知的玩家断线事件
 * 设置玩家离线状态，通知同桌其他玩家
 */
@ProcessType(CMsg.NOT_BREAK)
public class NotBreakHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(NotBreakHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			ServerProto.NotBreak notification = (ServerProto.NotBreak) message;
			int userId = notification.getUserId();
			int gateClientId = notification.getGateClientId();

			TraceContext.setUserId(userId);
			logger.info("处理玩家断线通知, userId: {}, gateClientId: {}", userId, gateClientId);
			Game.getInstance().getTableManager().findTablesByUserIdAsync(userId)
					.whenComplete((tables, error) -> {
						if (error != null) {
							logger.error("查找断线玩家所在桌子失败, userId: {}", userId, error);
							return;
						}
						for (Table table : tables) {
							table.execute(() -> processUserDisconnect(table, userId, gateClientId));
						}
					});
			return true;
		} catch (Exception e) {
			logger.error("处理玩家断线通知失败, clientId: {}", clientId, e);
			return false;
		}
	}

	private void processUserDisconnect(Table table, int userId, int gateClientId) {
			TableUser user = table.getUsers().get(userId);
			if (user == null) return;
			if (gateClientId != 0 && user.getGateId() != 0 && user.getGateId() != gateClientId) {
				logger.info("忽略旧连接断线, userId: {}, tableId: {}, noticeGate: {}, currentGate: {}",
						userId, table.getTableId(), gateClientId, user.getGateId());
				return;
			}
			user.setOnLine(false);
			TraceContext.setTableId(table.getTableId());
			logger.info("玩家标记离线, userId: {}, tableId: {}", userId, table.getTableId());
			if (table.gaming()) {
				logger.info("游戏进行中玩家断线, userId: {}, tableId: {}，等待超时自动处理",
						userId, table.getTableId());
			}
	}
}
