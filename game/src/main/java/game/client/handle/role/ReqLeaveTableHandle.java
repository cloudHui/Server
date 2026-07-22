package game.client.handle.role;

import com.google.protobuf.Message;
import game.Game;
import game.manager.table.Table;
import game.manager.table.TableUser;
import msg.annotation.ProcessType;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import net.client.Sender;
import net.handler.Handler;
import net.message.TCPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ConstProto;
import proto.GameProto;
import threadtutil.thread.Task;

/**
 * 处理玩家请求离开桌子
 */
@ProcessType(GMsg.REQ_LEAVE)
public class ReqLeaveTableHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqLeaveTableHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			logger.info("处理离开桌子请求, userId: {}, tableId: {}", clientId, mapId);

			Table table = Game.getInstance().getTableManager().getTable(mapId);
			if (table == null) {
				sender.sendMessage(TCPMessage.newInstance(ConstProto.Result.TABLE_NULL_VALUE));
				return true;
			}

			Game.getInstance().serialExecute(new Task() {
				@Override
				public int groupId() { return table.getGroupIndex(); }

				@Override
				public void run() {
					int result = processLeave(clientId, table);
					if (result == ConstProto.Result.SUCCESS_VALUE) {
						GameProto.AckLeaveTable response = GameProto.AckLeaveTable.newBuilder()
								.setTableInfo(GameProto.TableInfo.newBuilder()
										.setTableId(table.getTableId()).setRoomId(table.getRoomId()).build())
								.build();
						sender.sendMessage(clientId, GMsg.ACK_LEAVE, mapId, response, sequence);
					} else {
						sender.sendMessage(TCPMessage.newInstance(result));
					}
				}
			});
		} catch (Exception e) {
			logger.error("处理离开桌子请求失败, userId: {}", clientId, e);
		}
		return true;
	}

	private int processLeave(int userId, Table table) {
		TableUser user = table.getUsers().get(userId);
		if (user == null) return ConstProto.Result.ROLE_NULL_VALUE;

		long tableId = table.getTableId();

		// 游戏中离开: 解散牌局, 发送总结算
		if (table.gaming()) {
			logger.info("游戏中玩家离开, 解散牌局, userId: {}, tableId: {}", userId, tableId);
			if (table.isMultiRound()) {
				if (table.getGameType() == 1) {
					game.manager.table.mj.MjSettleService.sendGameResult(game.manager.table.MjTable.class.cast(table));
				} else {
					game.manager.table.ddz.DdzSettleService.sendGameResult(table);
				}
			}
			table.removeUser(user);
			Game.getInstance().getTableManager().removeTable(tableId);
			return ConstProto.Result.SUCCESS_VALUE;
		}

		table.removeUser(user);
		logger.info("等待阶段玩家离桌, userId: {}, tableId: {}, remain: {}, human: {}",
				userId, tableId, table.getUsers().size(), table.hasHumanPlayer());

		// 空桌或只剩机器人：立即销毁，并通知大厅
		if (table.isEmpty() || !table.hasHumanPlayer()
				|| table.getTableState() == TableState.TABLE_DIS) {
			Game.getInstance().getTableManager().removeTable(tableId);
		} else {
			// 仍有真人：同步大厅名单
			Game.getInstance().getTableManager().notifyRoomPlayerLeft(tableId, userId);
		}
		return ConstProto.Result.SUCCESS_VALUE;
	}
}
