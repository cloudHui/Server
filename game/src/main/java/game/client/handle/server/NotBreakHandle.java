package game.client.handle.server;

import java.util.List;

import com.google.protobuf.Message;
import game.Game;
import game.manager.table.Table;
import game.manager.table.TableUser;
import msg.annotation.ProcessType;
import msg.registor.enums.TableState;
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

			processUserDisconnect(userId, gateClientId);

			return true;
		} catch (Exception e) {
			logger.error("处理玩家断线通知失败, clientId: {}", clientId, e);
			return false;
		}
	}

	private void processUserDisconnect(int userId, int gateClientId) {
		List<Table> tables = Game.getInstance().getTableManager().findTablesByUserId(userId);

		if (tables.isEmpty()) {
			logger.debug("断线玩家不在任何桌子中, userId: {}", userId);
			return;
		}

		for (Table table : tables) {
			TableUser user = table.getUsers().get(userId);
			if (user == null) {
				continue;
			}

			// 已被新连接顶替：忽略旧连接断线
			if (gateClientId != 0 && user.getGateId() != 0 && user.getGateId() != gateClientId) {
				logger.info("忽略旧连接断线, userId: {}, tableId: {}, noticeGate: {}, currentGate: {}",
						userId, table.getTableId(), gateClientId, user.getGateId());
				continue;
			}

			user.setOnLine(false);
			TraceContext.setTableId(table.getTableId());
			logger.info("玩家标记离线, userId: {}, tableId: {}", userId, table.getTableId());

			if (table.gaming()) {
				logger.info("游戏进行中玩家断线, userId: {}, tableId: {}，等待超时自动处理",
						userId, table.getTableId());
			}

			if (table.getTableState() == TableState.WAITING && table.isEmpty()) {
				table.upNextState(TableState.TABLE_DIS);
				logger.info("等待阶段玩家全部离开，解散桌子, tableId: {}", table.getTableId());
			}
		}
	}
}
