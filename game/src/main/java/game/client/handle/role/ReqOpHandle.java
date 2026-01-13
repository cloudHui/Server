package game.client.handle.role;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;

import game.Game;
import game.manager.TableManager;
import game.manager.table.Table;
import msg.annotation.ProcessType;
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
					int result = processUserOp(clientId, request.getOp(), table, sender, sequence);
					if (result == ConstProto.Result.SUCCESS_VALUE) {
						// 发送响应
						sender.sendMessage(clientId, GMsg.ACK_OP, table.getTableId(),
							buildUserOpResponse(table, request.getOp(), request.getOp().getChoiceValue(), clientId),
							sequence);
					} else {
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
	private int processUserOp(int userId, GameProto.OpInfo op, Table table, Sender sender, int sequence) {
		try {

			if (!table.gaming()) {
				logger.error("桌子未开始, userId: {}, tableId: {}", userId, table.getTableId());
				return ConstProto.Result.TABLE_NOT_START_VALUE;
			}

			Set<GameProto.OpInfo> currChoice = table.getOp().getCurrChoice();
			if (currChoice.isEmpty()) {
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

			if (currOp.getOpCardsCount() != op.getOpCardsCount()) {
				logger.error("操作牌数不匹配, userId: {}, tableId: {}", userId, table.getTableId());
				return ConstProto.Result.OP_CARD_NOT_MATCH_VALUE;
			}

			//TODO 应该这么操作了
			return ConstProto.Result.SUCCESS_VALUE;
		} catch (Exception e) {
			logger.error("处理玩家操作请求失败, userId: {}", userId, e);
			return ConstProto.Result.SERVER_ERROR_VALUE;
		}
	}

	/**
	 * 构建玩家操作响应
	 */
	private GameProto.AckOp buildUserOpResponse(Table table, GameProto.OpInfo op, int opId, int clientId) {
		GameProto.AckOp.Builder response = GameProto.AckOp.newBuilder();
		response.setOp(op);
		response.setOpId(opId);
		response.setOpFrom(clientId);
		return response.build();
	}
}