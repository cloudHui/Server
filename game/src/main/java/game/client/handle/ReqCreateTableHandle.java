package game.client.handle;

import com.google.protobuf.Message;
import game.manager.TableManager;
import game.manager.model.Table;
import msg.annotation.ProcessType;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;

/**
 * 处理房间服务请求创建桌子
 * 负责创建游戏桌子和分配桌子ID
 */
@ProcessType(SMsg.REQ_CREATE_TABLE_MSG)
public class ReqCreateTableHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqCreateTableHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, int mapId, long sequence) {
		try {
			ServerProto.ReqCreateGameTable request = (ServerProto.ReqCreateGameTable) message;
			int roomId = request.getRoomId();

			logger.info("处理创建桌子请求, clientId: {}, roomId: {}", clientId, roomId);

			// 创建桌子并生成响应
			ServerProto.AckCreateGameTable response = createGameTable(roomId, request.getRoomRole());

			// 发送响应
			sender.sendMessage(clientId, SMsg.ACK_CREATE_TABLE_MSG, mapId, response, sequence);

			logger.info("创建桌子请求处理完成, clientId: {}, tableId: {}", clientId, response.getTables().getTableId());
			return true;
		} catch (Exception e) {
			logger.error("处理创建桌子请求失败, clientId: {}", clientId, e);
			return false;
		}
	}

	/**
	 * 创建游戏桌子
	 */
	private ServerProto.AckCreateGameTable createGameTable(int roomId, ServerProto.RoomRole role) {
		TableManager tableManager = game.Game.getInstance().getTableManager();

		// 生成桌子ID
		String tableId = tableManager.getTableId();

		// 创建桌子实例
		Table table = new Table(tableId, role);
		tableManager.addTable(table);

		// 启动桌子逻辑循环
		table.start();

		// 构建响应
		ServerProto.AckCreateGameTable.Builder response = ServerProto.AckCreateGameTable.newBuilder();
		response.setTables(ServerProto.RoomTableInfo.newBuilder()
				.setTableId(com.google.protobuf.ByteString.copyFromUtf8(tableId))
				.setRoomId(roomId)
				.build());

		logger.debug("创建游戏桌子成功, tableId: {}, roomId: {}", tableId, roomId);
		return response.build();
	}
}