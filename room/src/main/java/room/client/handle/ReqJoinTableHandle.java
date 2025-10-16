package room.client.handle;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
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
import proto.ConstProto;
import proto.RoomProto;
import proto.ServerProto;
import room.Room;
import room.client.ClientProto;
import room.manager.table.TableInfo;
import room.manager.table.TableManager;
import room.manager.user.User;
import room.manager.user.UserManager;

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
			logger.error("用户不存在, clientId: {}, 错误码: {}", clientId, ConstProto.Result.SERVER_ERROR_VALUE);
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
		boolean join = joinTable(tableModel, user, sender, sequence);
		if (!join) {
			// 4. 获取游戏服务器连接
			ConnectHandler gameServer = Room.getInstance().getServerManager().getServerClient(ServerType.Game);
			if (gameServer == null) {
				logger.error("游戏服务器不可用, 错误码: {}", ConstProto.Result.SERVER_NULL_VALUE);
				sender.sendMessage(TCPMessage.newInstance(ConstProto.Result.SERVER_NULL_VALUE));
				return true;
			}
			// 4. 执行创建桌子逻辑
			createTable(gameServer, roomId, sender, sequence, user);
		}
		return true;
	}


	/**
	 * 加入桌子
	 */
	private boolean joinTable(TableModel tableModel, User user, Sender sender, long sequence) {
		TableInfo canJoinTable = TableManager.getInstance().getCanJoinTable(tableModel.getId());
		if (canJoinTable == null) {
			return false;
		}
		canJoinTable.joinRole(user);

		sendJoinTableAck(canJoinTable.getTableId(), sender, sequence, user.getUserId());
		return true;
	}

	/**
	 * 执行实际的创建桌子操作
	 * 向游戏服务器发送创建桌子的请求，并处理响应
	 *
	 * @param gameServer 游戏服务器连接处理器
	 * @param roomId     房间ID
	 * @param sender     消息发送器
	 * @param sequence   序列号
	 */
	private void createTable(ConnectHandler gameServer, int roomId, Sender sender, long sequence, User user) {
		// 向游戏服务器发送请求并处理响应
		gameServer.sendMessageBackTcp(buildCreateTableRequest(roomId, user.getUserId()), SMsg.REQ_CREATE_TABLE_MSG, GAME_SERVER_TIMEOUT)
				.whenComplete((response, error) -> {
					// 处理网络错误
					if (error != null) {
						logger.error("向游戏服务器发送创建房间请求失败: {}", error.getMessage());
						sender.sendMessage(TCPMessage.newInstance(ConstProto.Result.SERVER_ERROR_VALUE));
						return;
					}

					if (response.getResult() != ConstProto.Result.SUCCESS_VALUE) {
						logger.error("游戏服务器返回了错误: {}", response.getResult());
						sender.sendMessage(TCPMessage.newInstance(response.getResult()));
						return;
					}
					try {
						Message message = ClientProto.PARSER.parser(response.getMessageId(), response.getMessage());
						//Todo 应该写成 各个处理器自己处理的前面的错误都是统一的 和tool 的 sendMsg合并一下
						if (!(message instanceof ServerProto.AckCreateGameTable)) {
							logger.error("游戏服务器返回了错误的响应类型: {}", message != null ? response.getClass().getSimpleName() : "null");
							sender.sendMessage(TCPMessage.newInstance(ConstProto.Result.SERVER_ERROR_VALUE));
							return;
						}
						//发送加入桌子成功的响应给客户端
						dealCreateSuccessTableJoin(sender, sequence, (ServerProto.AckCreateGameTable) message, user);
					} catch (InvalidProtocolBufferException e) {
						e.printStackTrace();
					}
				});
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
	 * 发送加入桌子成功的响应给客户端
	 */
	private void dealCreateSuccessTableJoin(Sender sender, long sequence, ServerProto.AckCreateGameTable ack, User user) {
		TableInfo tableInfo = TableManager.getInstance().putRoomInfo(ack.getTables());
		tableInfo.joinRole(user);
		// 处理成功响应
		sendJoinTableAck(ack.getTables().getTableId().toStringUtf8(), sender, sequence, user.getUserId());
	}


	/**
	 * 发送 玩家加入room桌子回复
	 */
	private void sendJoinTableAck(String tableId, Sender sender, long sequence, int userId) {
		RoomProto.AckJoinRoomTable response = RoomProto.AckJoinRoomTable.newBuilder()
				.setTableId(ByteString.copyFromUtf8(tableId))
				.build();
		sender.sendMessage(0, RMsg.ACK_JOIN_ROOM_TABLE_MSG, 0, response, sequence);
		logger.info("玩家加入桌子成功, userId: {}, tableId: {}", userId, tableId);
	}
}