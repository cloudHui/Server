package game.client.handle.server;

import com.google.protobuf.Message;
import game.Game;
import game.manager.TableManager;
import game.manager.table.Table;
import msg.annotation.ProcessType;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import proto.ServerProto;

/**
 * 处理房间服务请求创建桌子
 * 负责创建游戏桌子和分配桌子ID
 */
@ProcessType(SMsg.REQ_CREATE_TABLE_MSG)
public class ReqCreateTableHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqCreateTableHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		final int roomId;
		final ModelProto.RoomRole role;
		try {
			ServerProto.ReqCreateGameTable request = (ServerProto.ReqCreateGameTable) message;
			roomId = request.getRoomId();
			role = request.getRoomRole();
			logger.info("处理创建桌子请求, clientId: {}, roomId: {}", clientId, roomId);

		} catch (Exception e) {
			logger.error("处理创建桌子请求失败, clientId: {}", clientId, e);
			return true;
		}
		Game.getInstance().getTableManager().createTableAsync(roomId, role)
				.whenComplete((table, error) -> sendCreateResponse(sender, clientId, mapId, sequence, table, error));
		return true;
	}

	/** 在生命周期线程完成后返回结果，网络线程不阻塞等待桌子创建。 */
	private void sendCreateResponse(Sender sender, int clientId, long mapId, int sequence,
			Table table, Throwable error) {
		if (error != null || table == null) {
			logger.error("创建桌子失败, clientId: {}", clientId, error);
			sender.sendMessage(clientId, SMsg.ACK_CREATE_TABLE_MSG, mapId,
					ServerProto.AckCreateGameTable.getDefaultInstance(), sequence);
			return;
		}
		ServerProto.AckCreateGameTable response = ServerProto.AckCreateGameTable.newBuilder()
				.setTables(ModelProto.RoomTableInfo.newBuilder().setTableId(table.getTableId())
						.setRoomId(table.getRoomId()).build()).build();
		sender.sendMessage(clientId, SMsg.ACK_CREATE_TABLE_MSG, mapId, response, sequence);
		logger.info("创建桌子请求处理完成, clientId: {}, tableId: {}", clientId, table.getTableId());
	}
}
