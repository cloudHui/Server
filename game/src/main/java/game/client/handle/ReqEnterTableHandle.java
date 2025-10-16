package game.client.handle;

import com.google.protobuf.Message;
import game.manager.TableManager;
import game.manager.model.GameUser;
import game.manager.model.Table;
import msg.annotation.ProcessType;
import msg.registor.message.GMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GameProto;

/**
 * 处理玩家请求进入桌子
 * 负责验证玩家和桌子状态，处理入桌逻辑
 */
@ProcessType(GMsg.REQ_ENTER_TABLE_MSG)
public class ReqEnterTableHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqEnterTableHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, int mapId, int sequence) {
		try {
			GameProto.ReqEnterTable request = (GameProto.ReqEnterTable) message;
			String tableId = request.getTableId().toStringUtf8();

			logger.info("处理进入桌子请求, userId: {}, tableId: {}", clientId, tableId);

			// 处理进入桌子逻辑
			boolean success = processEnterTable(clientId, tableId);

			// 构建响应
			GameProto.AckEnterTable response = buildEnterTableResponse();

			// 发送响应
			sender.sendMessage(clientId, GMsg.ACK_ENTER_TABLE_MSG, mapId, response, sequence);

			logger.info("进入桌子请求处理完成, userId: {}, tableId: {}, success: {}", clientId, tableId, success);
			return true;
		} catch (Exception e) {
			logger.error("处理进入桌子请求失败, userId: {}", clientId, e);
			return false;
		}
	}

	/**
	 * 处理进入桌子逻辑
	 */
	private boolean processEnterTable(int userId, String tableId) {
		try {
			// 获取桌子管理器
			TableManager tableManager = game.Game.getInstance().getTableManager();

			// 查找桌子
			Table table = tableManager.getTable(tableId);
			if (table == null) {
				logger.warn("桌子不存在, tableId: {}", tableId);
				return false;
			}

			// 创建用户对象
			GameUser user = new GameUser();
			user.setUserId(userId);
			user.setOnLine(true);

			// 尝试加入桌子
			boolean success = table.addUser(user);
			if (success) {
				logger.debug("用户成功加入桌子, userId: {}, tableId: {}", userId, tableId);
				user.addTable(tableId);
			} else {
				logger.warn("用户加入桌子失败, userId: {}, tableId: {}", userId, tableId);
			}

			return success;
		} catch (Exception e) {
			logger.error("处理进入桌子逻辑失败, userId: {}, tableId: {}", userId, tableId, e);
			return false;
		}
	}

	/**
	 * 构建进入桌子响应
	 */
	private GameProto.AckEnterTable buildEnterTableResponse() {
		GameProto.AckEnterTable.Builder response = GameProto.AckEnterTable.newBuilder();
		// 可以添加更多响应字段，如错误码、提示信息等
		return response.build();
	}
}