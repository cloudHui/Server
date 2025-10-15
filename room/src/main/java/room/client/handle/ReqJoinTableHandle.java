package room.client.handle;

import com.google.protobuf.Message;
import model.TableModel;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.RMsg;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.connect.handle.ConnectHandler;
import net.handler.Handler;
import net.message.TCPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ResultProto;
import proto.RoomProto;
import proto.ServerProto;
import room.Room;
import room.manager.RoomManager;
import room.manager.User;
import room.manager.UserManager;

/**
 * 处理玩家请求进入桌子的处理器
 * 负责验证用户和房间信息，并向游戏服务器发送创建桌子请求
 */
@ProcessType(RMsg.REQ_JOIN_ROOM_TABLE_MSG)
public class ReqJoinTableHandle implements Handler {

	private static final Logger logger = LoggerFactory.getLogger(ReqJoinTableHandle.class);

	// 游戏服务器请求超时时间（秒）
	private static final int GAME_SERVER_TIMEOUT = 3;

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		// 1. 验证用户是否存在
		User user = UserManager.getInstance().getUser(clientId);
		if (user == null) {
			logger.error("用户不存在, clientId: {}, 错误码: {}", clientId, ResultProto.Result.SERVER_ERROR_VALUE);
			sender.sendMessage(TCPMessage.newInstance(ResultProto.Result.SERVER_ERROR_VALUE));
			return true;
		}

		// 2. 解析请求并验证房间配置
		RoomProto.ReqJoinRoomTable request = (RoomProto.ReqJoinRoomTable) msg;
		int roomId = request.getRoomId();

		TableModel tableModel = RoomManager.getInstance().getTableModel(roomId);
		if (tableModel == null) {
			logger.error("房间配置不存在, roomId: {}, 错误码: {}", roomId, ResultProto.Result.TABLE_CONFIG_ERROR_VALUE);
			sender.sendMessage(TCPMessage.newInstance(ResultProto.Result.TABLE_CONFIG_ERROR_VALUE));
			return true;
		}

		// 3. 获取游戏服务器连接
		ConnectHandler gameServer = Room.getInstance().getServerManager().getServerClient(ServerType.Game);
		if (gameServer == null) {
			logger.error("游戏服务器不可用, 错误码: {}", ResultProto.Result.SERVER_NULL_VALUE);
			sender.sendMessage(TCPMessage.newInstance(ResultProto.Result.SERVER_NULL_VALUE));
			return true;
		}

		// 4. 执行加入桌子逻辑
		joinTable(gameServer, roomId, clientId, sender, mapId, sequence);
		return true;
	}

	/**
	 * 执行实际的加入桌子操作
	 * 向游戏服务器发送创建桌子的请求，并处理响应
	 *
	 * @param gameServer 游戏服务器连接处理器
	 * @param roomId     房间ID
	 * @param clientId   客户端ID
	 * @param sender     消息发送器
	 * @param mapId      地图ID
	 * @param sequence   序列号
	 */
	private void joinTable(ConnectHandler gameServer, int roomId, int clientId, Sender sender, int mapId, long sequence) {
		// 向游戏服务器发送请求并处理响应
		gameServer.sendMessage(buildCreateTableRequest(roomId, clientId), SMsg.REQ_CREATE_TABLE_MSG, GAME_SERVER_TIMEOUT)
				.whenComplete((response, error) -> handleCreateTableResponse(response, error, sender, clientId, mapId, sequence));
	}

	/**
	 * 构建创建游戏桌子的请求对象
	 */
	private ServerProto.ReqCreateGameTable buildCreateTableRequest(int roomId, int clientId) {
		return ServerProto.ReqCreateGameTable.newBuilder()
				.setRoomId(roomId)
				.setRoomRole(ServerProto.RoomRole.newBuilder()
						.setRoleId(clientId)
						.build())
				.build();
	}

	/**
	 * 处理创建桌子的响应
	 */
	private void handleCreateTableResponse(Message response, Throwable error, Sender sender, int clientId, int mapId, long sequence) {
		// 处理网络错误
		if (error != null) {
			logger.error("向游戏服务器发送创建房间请求失败: {}", error.getMessage());
			sender.sendMessage(TCPMessage.newInstance(ResultProto.Result.SERVER_ERROR_VALUE));
			return;
		}

		// 验证响应类型
		if (!(response instanceof ServerProto.AckCreateGameTable)) {
			logger.error("游戏服务器返回了错误的响应类型: {}", response != null ? response.getClass().getSimpleName() : "null");
			sender.sendMessage(TCPMessage.newInstance(ResultProto.Result.SERVER_ERROR_VALUE));
			return;
		}

		// 处理成功响应
		sendJoinTableSuccessResponse(sender, clientId, mapId, sequence, (ServerProto.AckCreateGameTable) response);
	}

	/**
	 * 发送加入桌子成功的响应给客户端
	 */
	private void sendJoinTableSuccessResponse(Sender sender, int clientId, int mapId, long sequence, ServerProto.AckCreateGameTable ack) {
		RoomProto.AckJoinRoomTable response = RoomProto.AckJoinRoomTable.newBuilder()
				.setTableId(ack.getTables().getTableId())
				.build();

		sender.sendMessage(clientId, RMsg.ACK_JOIN_ROOM_TABLE_MSG, mapId, response, sequence);
		logger.info("玩家加入桌子成功, clientId: {}, tableId: {}", clientId, ack.getTables().getTableId());
	}
}