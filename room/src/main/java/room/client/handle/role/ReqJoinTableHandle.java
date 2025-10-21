package room.client.handle.role;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import model.TableModel;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.RMsg;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.client.handler.ClientHandler;
import net.connect.handle.ConnectHandler;
import net.handler.Handler;
import net.message.TCPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ConstProto;
import proto.RoomProto;
import proto.ServerProto;
import room.Room;
import room.client.ClientProto;
import room.manager.table.TableInfo;
import room.manager.table.TableManager;
import room.manager.user.User;
import room.manager.user.UserManager;
import utils.manager.HandleManager;

/**
 * 处理玩家请求进入桌子的处理器
 * 负责验证用户和房间信息,并向游戏服务器发送创建桌子请求
 */
@ProcessType(RMsg.REQ_JOIN_ROOM_TABLE_MSG)
public class ReqJoinTableHandle implements Handler {

	private static final Logger logger = LoggerFactory.getLogger(ReqJoinTableHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, long mapId, int sequence) {
		// 1. 验证用户是否存在
		User user = UserManager.getInstance().getUser(clientId);
		if (user == null) {
			logger.error("用户不存在, userId: {}, 错误码: {}", mapId, ConstProto.Result.SERVER_ERROR_VALUE);
			sender.sendMessage(TCPMessage.newInstance(ConstProto.Result.SERVER_ERROR_VALUE));
			return true;
		}

		// 2. 解析请求并验证房间配置
		RoomProto.ReqJoinRoomTable request = (RoomProto.ReqJoinRoomTable) msg;
		int roomId = request.getRoomId();

		TableModel tableModel = TableManager.getInstance().getTableModel(roomId);
		if (tableModel == null) {
			logger.error("房间配置不存在, roomId: {}, 错误码: {}", roomId, ConstProto.Result.TABLE_CONFIG_ERROR_VALUE);
			sender.sendMessage(TCPMessage.newInstance(ConstProto.Result.TABLE_CONFIG_ERROR_VALUE));
			return true;
		}

		// 3. 能否加入旧房间
		boolean join = joinTable(tableModel, user, sequence);
		if (!join) {
			// 4. 获取游戏服务器连接
			ConnectHandler gameServer = Room.getInstance().getServerManager().getServerClient(ServerType.Game);
			if (gameServer == null) {
				logger.error("游戏服务器不可用, 错误码: {}", ConstProto.Result.SERVER_NULL_VALUE);
				sender.sendMessage(TCPMessage.newInstance(ConstProto.Result.SERVER_NULL_VALUE));
				return true;
			}
			// 4. 执行创建桌子逻辑
			createTable(gameServer, roomId, sequence, clientId);
		}
		return true;
	}


	/**
	 * 加入桌子
	 */
	private boolean joinTable(TableModel tableModel, User user, int sequence) {
		TableInfo canJoinTable = TableManager.getInstance().getCanJoinTable(tableModel.getId());
		if (canJoinTable == null) {
			return false;
		}
		canJoinTable.joinRole(user);

		sendJoinTableAck(canJoinTable.getTableId(), sequence, user);
		return true;
	}

	/**
	 * 执行实际的创建桌子操作
	 * 向游戏服务器发送创建桌子的请求,并处理响应
	 *
	 * @param gameServer 游戏服务器连接处理器
	 * @param roomId     房间ID
	 * @param sequence   序列号
	 * @param userId     用户ID
	 */
	private void createTable(ConnectHandler gameServer, int roomId, long sequence, int userId) {
		// 使用HandleManager统一发送和处理消息
		HandleManager.sendMsg(
				SMsg.REQ_CREATE_TABLE_MSG,
				buildCreateTableRequest(roomId, userId),
				gameServer,
				ClientProto.PARSER,
				(int) sequence,
				userId,
				true
		);
	}

	/**
	 * 构建创建游戏桌子的请求对象
	 */
	private ServerProto.ReqCreateGameTable buildCreateTableRequest(int roomId, int userId) {
		return ServerProto.ReqCreateGameTable.newBuilder()
				.setRoomId(roomId)
				.setRoomRole(ServerProto.RoomRole.newBuilder()
						.setRoleId(userId)
						.build())
				.build();
	}

	/**
	 * 发送 玩家加入room桌子回复
	 */
	public static void sendJoinTableAck(long tableId, int sequence, User user) {

		ClientHandler gate = Room.getInstance().getServerClientManager().getServerClient(ServerType.Gate, user.getClientId());
		if (gate == null) {
			logger.error("sendJoinTableAck role:{} gate:{} null", user.getUserId(), user.getClientId());
			return;
		}
		gate.sendMessage(RMsg.ACK_JOIN_ROOM_TABLE_MSG, RoomProto.AckJoinRoomTable.newBuilder()
				.setTableId(tableId)
				.build(), sequence);
		logger.info("玩家加入桌子成功, userId: {}, tableId: {}", user.getUserId(), tableId);
	}
}