package game.client.handle.role;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;

import game.Game;
import game.manager.TableManager;
import game.manager.table.Table;
import game.manager.table.ddz.DdzBidService;
import game.manager.table.ddz.DdzPlayService;
import msg.annotation.ProcessType;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import net.client.Sender;
import net.handler.Handler;
import net.message.TCPMessage;
import proto.ConstProto;
import proto.GameProto;
import threadtutil.thread.Task;

/**
 * 处理玩家操作请求
 */
@ProcessType(GMsg.REQ_OP)
public class ReqOpHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqOpHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			GameProto.ReqOp request = (GameProto.ReqOp)message;

			logger.info("处理玩家操作请求, userId: {}, tableId: {}", clientId, mapId);

			// 获取桌子管理器
			TableManager tableManager = Game.getInstance().getTableManager();

			// 查找桌子
			Table table = tableManager.getTable(mapId);
			if (table == null) {
				logger.warn("桌子不存在, tableId: {}", mapId);
				sender.sendMessage(TCPMessage.newInstance(ConstProto.Result.TABLE_NULL_VALUE));
				return true;
			}
			Game.getInstance().serialExecute(new Task() {
				@Override
				public int groupId() {
					return table.getGroupIndex();
				}

				@Override
				public void run() {
					// 处理玩家在桌子上的操作逻辑
					int result = processUserOp(clientId, request.getOp(), table);
					if (result != ConstProto.Result.SUCCESS_VALUE) {
						sender.sendMessage(TCPMessage.newInstance(result));
					}

					logger.info("玩家操作请求处理完成, userId: {}, tableId: {}, success: {}", clientId, mapId, result);
				}
			});

		} catch (Exception e) {
			logger.error("处理进入桌子请求失败, userId: {}", mapId, e);
		}
		return true;
	}

	/**
	 * 处理玩家操作逻辑
	 */
	private int processUserOp(int userId, GameProto.OpInfo op, Table table) {
		try {
			TableState ts = table.getTableState();
			if (ts != TableState.IDLE_ROB && ts != TableState.IDLE_CARD) {
				return ConstProto.Result.OP_CURR_ERROR_VALUE;
			}
			if (!table.gaming()) {
				logger.error("桌子未开始, userId: {}, tableId: {}", userId, table.getTableId());
				return ConstProto.Result.TABLE_NOT_START_VALUE;
			}

			Set<GameProto.OpInfo> currChoice = table.getOp().getCurrChoice();
			if (currChoice == null || currChoice.isEmpty()) {
				logger.error("当前操作位置没有操作, userId: {}, tableId: {}", userId, table.getTableId());
				return ConstProto.Result.OP_CURR_ERROR_VALUE;
			}
			GameProto.OpInfo currOp = null;
			for (GameProto.OpInfo temp : currChoice) {
				if (op.getChoiceValue() == temp.getChoiceValue()) {
					currOp = temp;
					break;
				}
			}
			if (currOp == null) {
				logger.error("当前操作位置没有操作, userId: {}, tableId: {}", userId, table.getTableId());
				return ConstProto.Result.OP_CURR_ERROR_VALUE;
			}

			if (currOp.getOpCardsCount() > 0 && currOp.getOpCardsCount() != op.getOpCardsCount()) {
				logger.error("操作牌数不匹配, userId: {}, tableId: {}", userId, table.getTableId());
				return ConstProto.Result.OP_CARD_NOT_MATCH_VALUE;
			}

			if (ts == TableState.IDLE_ROB) {
				return DdzBidService.apply(table, userId, op);
			}
			return DdzPlayService.apply(table, userId, op);
		} catch (Exception e) {
			logger.error("处理玩家操作请求失败, userId: {}", userId, e);
			return ConstProto.Result.SERVER_ERROR_VALUE;
		}
	}
}