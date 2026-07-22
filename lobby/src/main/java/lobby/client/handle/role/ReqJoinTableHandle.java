package lobby.client.handle.role;

import com.google.protobuf.Message;
import model.tablemodel.TableModel;
import model.tablemodel.TableModelJson;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.LMsg;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.client.handler.ClientHandler;
import net.connect.handle.ConnectHandler;
import net.handler.Handler;
import net.message.TCPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ConstProto;
import proto.LobbyProto;
import proto.ModelProto;
import proto.ServerProto;
import lobby.Lobby;
import lobby.client.ClientProto;
import lobby.manager.User;
import lobby.manager.UserManager;
import lobby.manager.table.TableInfo;
import lobby.manager.table.TableManager;
import tools.manager.HandleManager;

@ProcessType(LMsg.REQ_JOIN_ROOM_TABLE_MSG)
public class ReqJoinTableHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqJoinTableHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, long mapId, int sequence) {
		User user = UserManager.getInstance().getUser(clientId);
		if (user == null) {
			logger.error("用户不存在, userId: {}", mapId);
			sender.sendMessage(TCPMessage.newInstance(ConstProto.Result.SERVER_ERROR_VALUE));
			return true;
		}

		LobbyProto.ReqJoinRoomTable request = (LobbyProto.ReqJoinRoomTable) msg;
		int roomId = request.getRoomId();
		TableModel tableModel = TableManager.getInstance().getTableModel(roomId);
		if (tableModel == null) {
			logger.error("房间配置不存在, roomId: {}", roomId);
			sender.sendMessage(TCPMessage.newInstance(ConstProto.Result.TABLE_CONFIG_ERROR_VALUE));
			return true;
		}

		if (!joinTable(tableModel, user, sequence)) {
			ConnectHandler gameServer = Lobby.getInstance().getServerManager().getServerClient(ServerType.Game);
			if (gameServer == null) {
				logger.error("游戏服务器不可用");
				sender.sendMessage(TCPMessage.newInstance(ConstProto.Result.SERVER_NULL_VALUE));
				return true;
			}
			createTable(gameServer, roomId, sequence, clientId);
		}
		return true;
	}

	private boolean joinTable(TableModel tableModel, User user, int sequence) {
		TableInfo canJoinTable = TableManager.getInstance().getCanJoinTable(tableModel.getId());
		if (canJoinTable == null) {
			return false;
		}
		canJoinTable.joinRole(user);
		sendJoinTableAck(canJoinTable.getTableId(), sequence, user);
		return true;
	}

	private void createTable(ConnectHandler gameServer, int roomId, long sequence, int userId) {
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

	private ServerProto.ReqCreateGameTable buildCreateTableRequest(int roomId, int userId) {
		ModelProto.RoomRole.Builder role = ModelProto.RoomRole.newBuilder().setRoleId(userId);
		TableModel model = TableManager.getInstance().getTableModel(roomId);
		if (model != null && model.getId() >= 10000) {
			role.setAvatar(com.google.protobuf.ByteString.copyFromUtf8(
					"TMJSON:" + TableModelJson.toJson(model)));
		}
		return ServerProto.ReqCreateGameTable.newBuilder()
				.setRoomId(roomId)
				.setRoomRole(role.build())
				.build();
	}

	public static void sendJoinTableAck(long tableId, int sequence, User user) {
		ClientHandler gate = Lobby.getInstance().getServerClientManager()
				.getServerClient(ServerType.Gate, user.getClientId());
		if (gate == null) {
			gate = Lobby.getInstance().getServerClientManager().getServerClient(ServerType.Gate);
		}
		if (gate == null) {
			logger.error("sendJoinTableAck role:{} gate null", user.getUserId());
			return;
		}
		gate.sendMessage(LMsg.ACK_JOIN_ROOM_TABLE_MSG, LobbyProto.AckJoinRoomTable.newBuilder()
				.setTableId(tableId)
				.build(), sequence);
		logger.info("玩家加入桌子成功, userId: {}, tableId: {}", user.getUserId(), tableId);
	}
}
