package game.client.handle.role;

import com.google.protobuf.Message;
import game.Game;
import game.manager.TableManager;
import game.manager.table.Table;
import msg.annotation.ProcessType;
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
 * 处理玩家操作请求
 */
@ProcessType(GMsg.REQ_OP)
public class ReqOpHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqOpHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			GameProto.ReqOp request = (GameProto.ReqOp) message;

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
					return (int) (mapId / Game.getInstance().getPooSize());
				}

				@Override
				public void run() {
					// 处理进入桌子逻辑

					//logger.info("进入桌子请求处理完成, userId: {}, tableId: {}, success: {}", mapId, tableId, result);
				}
			});

		} catch (Exception e) {
			logger.error("处理进入桌子请求失败, userId: {}", mapId, e);
		}
		return true;
	}
}