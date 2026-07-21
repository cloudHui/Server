package game.client.handle.role;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import game.Game;
import game.manager.TableManager;
import game.manager.table.Table;
import game.manager.table.TableUser;
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
 * 处理玩家请求进入桌子
 */
@ProcessType(GMsg.REQ_ENTER_TABLE_MSG)
public class ReqEnterTableHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqEnterTableHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			GameProto.ReqEnterTable request = (GameProto.ReqEnterTable) message;
			logger.info("处理进入桌子请求, userId: {}, tableId: {}", clientId, request.getTableId());

			TableManager tableManager = Game.getInstance().getTableManager();
			Table table = tableManager.getTable(request.getTableId());
			if (table == null) {
				logger.warn("桌子不存在, tableId: {}", request.getTableId());
				sender.sendMessage(TCPMessage.newInstance(ConstProto.Result.TABLE_NULL_VALUE));
				return true;
			}

			Game.getInstance().serialExecute(new Task() {
				@Override
				public int groupId() { return table.getGroupIndex(); }

				@Override
				public void run() {
					int result = processEnterTable(clientId, request.getTableId(), clientId, request, table);
					if (result == ConstProto.Result.SUCCESS_VALUE) {
						GameProto.AckEnterTable response = buildEnterTableResponse(table);
						sender.sendMessage(clientId, GMsg.ACK_ENTER_TABLE_MSG, request.getTableId(), response, sequence);
					} else {
						// 必须带原 sequence，否则 web sendAndWait 会一直等到超时
						GameProto.AckEnterTable empty = GameProto.AckEnterTable.newBuilder().build();
						sender.sendMessage(clientId, GMsg.ACK_ENTER_TABLE_MSG, request.getTableId(), empty, sequence);
						logger.warn("进入桌子失败, userId: {}, tableId: {}, result: {}", clientId, request.getTableId(), result);
					}
					logger.info("进入桌子请求处理完成, userId: {}, tableId: {}, success: {}", clientId, request.getTableId(), result);
				}
			});
		} catch (Exception e) {
			logger.error("处理进入桌子请求失败, userId: {}", mapId, e);
		}
		return true;
	}

	private int processEnterTable(int userId, long tableId, int gateId, GameProto.ReqEnterTable req, Table table) {
		try {
			// 游戏中: 检查断线重连
			if (table.gaming()) {
				TableUser existingUser = table.getUsers().get(userId);
				if (existingUser != null) {
					logger.info("玩家断线重连, userId: {}, tableId: {}, gateId: {} -> {}",
							userId, tableId, existingUser.getGateId(), gateId);
					existingUser.setOnLine(true);
					existingUser.setGateId(gateId);
					table.syncGameState(existingUser); // 多态调用
					return ConstProto.Result.SUCCESS_VALUE;
				}
				return ConstProto.Result.TABLE_START_VALUE;
			}

			TableUser user = table.getUser(userId, gateId, req);
			int result = table.addUser(user);
			if (result == ConstProto.Result.SUCCESS_VALUE) {
				user.addTable(tableId);
				return result;
			}
			table.getUsers().remove(userId);
			return result;
		} catch (Exception e) {
			logger.error("处理进入桌子逻辑失败, userId: {}, tableId: {}", userId, tableId, e);
			return ConstProto.Result.TABLE_ERROR_VALUE;
		}
	}

	private GameProto.AckEnterTable buildEnterTableResponse(Table table) {
		GameProto.AckEnterTable.Builder response = GameProto.AckEnterTable.newBuilder();
		response.setTableInfo(GameProto.TableInfo.newBuilder()
				.setTableId(table.getTableId()).setRoomId(table.getRoomId()).build());
		for (TableUser tableUser : table.getUsers().values()) {
			response.addPlayers(GameProto.Player.newBuilder()
					.setPosition(tableUser.getSeated())
					.setRoleId(tableUser.getUserId())
					.setNickName(ByteString.copyFromUtf8(tableUser.getNick()))
					.setAvatar(ByteString.copyFromUtf8(tableUser.getHead()))
					.build());
		}
		return response.build();
	}
}
