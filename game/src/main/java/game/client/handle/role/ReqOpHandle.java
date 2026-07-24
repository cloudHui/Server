package game.client.handle.role;

import game.manager.table.Table;
import game.manager.table.state.TableSettleSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;

import game.Game;
import game.manager.TableManager;
import msg.annotation.ProcessType;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import net.client.Sender;
import net.handler.Handler;
import net.message.TCPMessage;
import proto.ConstProto;
import proto.GameProto;

/**
 * 处理玩家操作请求
 * 通用校验后调用 table.processOp() 多态分发到具体玩法
 */
@ProcessType(GMsg.REQ_OP)
public class ReqOpHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqOpHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			GameProto.ReqOp request = (GameProto.ReqOp)message;
			logger.info("处理玩家操作请求, userId: {}, tableId: {}", clientId, mapId);

			TableManager tableManager = Game.getInstance().getTableManager();
			Table table = tableManager.getTable(mapId);
			if (table == null) {
				logger.warn("桌子不存在, tableId: {}", mapId);
				sender.sendMessage(TCPMessage.newInstance(ConstProto.Result.TABLE_NULL_VALUE));
				return true;
			}

            table.execute(() -> {
					int result = processUserOp(clientId, request.getOp(), table, sender, mapId, sequence);
					if (result != ConstProto.Result.SUCCESS_VALUE) {
						sender.sendMessage(TCPMessage.newInstance(result));
					} else {
						// 成功时桌内已广播 ACK_OP(无 sequence)；这里必须带原 sequence 回给请求方，
						// 否则 Gate 3 秒超时会向客户端推 TIME_OUT，牌桌中央就会反复出现「操作超时」。
						GameProto.AckOp ack = GameProto.AckOp.newBuilder()
								.setOp(request.getOp())
								.setOpId(clientId)
								.setOpFrom(clientId)
								.build();
						sender.sendMessage(clientId, GMsg.ACK_OP, mapId, ack, sequence);
					}
					logger.info("玩家操作请求处理完成, userId: {}, tableId: {}, success: {}", clientId, mapId, result);
			}).exceptionally(error -> {
				logger.error("桌子线程处理玩家操作失败, tableId: {}", mapId, error);
				return null;
			});
		} catch (Exception e) {
			logger.error("处理操作请求失败, userId: {}", mapId, e);
		}
		return true;
	}

	private int processUserOp(int userId, GameProto.OpInfo op, Table table, Sender sender, long mapId, int sequence) {
		try {
			TableState ts = table.getTableState();

			// TABLE_OVER状态: 处理准备下一局
			if (ts == TableState.TABLE_OVER) {
				return processPrepare(table, userId, op);
			}

			if (!table.gaming()) {
				return ConstProto.Result.TABLE_NOT_START_VALUE;
			}

			// 多态分发到具体玩法
			return table.processOp(userId, op, sender, mapId, sequence);
		} catch (Exception e) {
			logger.error("处理玩家操作请求失败, userId: {}", userId, e);
			return ConstProto.Result.SERVER_ERROR_VALUE;
		}
	}

	private int processPrepare(Table table, int userId, GameProto.OpInfo op) {
		if (op.getChoice() != ConstProto.Operation.PREPARE) {
			return ConstProto.Result.OP_CURR_ERROR_VALUE;
		}

		table.addReady(userId);
		logger.info("玩家准备下一局, userId: {}, tableId: {}, ready: {}/{}",
				userId, table.getTableId(), table.getReadyCount(), table.getTableModel().getSeatNum());

		GameProto.AckOp ackOp = GameProto.AckOp.newBuilder()
				.setOp(GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.PREPARE).build())
				.setOpId(userId).build();
		table.sendTableMessage(ackOp, GMsg.ACK_OP);

		if (table.allReady()) {
			if (table.isLastRound()) {
				TableSettleSupport.sendFinalResultAndRemove(table);
				logger.info("最后一局完成, 总结算已发送, tableId: {}", table.getTableId());
			} else {
				logger.info("所有玩家已准备, 开始下一局, tableId: {}", table.getTableId());
				table.resetForNextRound();
				table.upNextState(TableState.WAITING);
			}
		}
		return ConstProto.Result.SUCCESS_VALUE;
	}
}
