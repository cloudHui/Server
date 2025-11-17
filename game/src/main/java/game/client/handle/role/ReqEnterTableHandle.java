package game.client.handle.role;

import java.util.Map;

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
 * 负责验证玩家和桌子状态,处理入桌逻辑
 */
@ProcessType(GMsg.REQ_ENTER_TABLE_MSG)
public class ReqEnterTableHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqEnterTableHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			GameProto.ReqEnterTable request = (GameProto.ReqEnterTable) message;
			logger.info("处理进入桌子请求, userId: {}, tableId: {}", clientId, request.getTableId());

			// 获取桌子管理器
			TableManager tableManager = Game.getInstance().getTableManager();

			// 查找桌子
			Table table = tableManager.getTable(request.getTableId());
			if (table == null) {
				logger.warn("桌子不存在, tableId: {}", request.getTableId());
				sender.sendMessage(TCPMessage.newInstance(ConstProto.Result.TABLE_NULL_VALUE));
				return true;
			}
			Game.getInstance().serialExecute(new Task() {
				@Override
				public int groupId() {
					return (int) (request.getTableId() / Game.getInstance().getPooSize());
				}

				@Override
				public void run() {
					// 处理进入桌子逻辑
					int result = processEnterTable(clientId, request.getTableId(), clientId, request, table);

					if (result == ConstProto.Result.SUCCESS_VALUE) {
						// 构建响应
						GameProto.AckEnterTable response = buildEnterTableResponse(table);
						// 发送响应
						sender.sendMessage(clientId, GMsg.ACK_ENTER_TABLE_MSG, request.getTableId(), response, sequence);
					} else {
						sender.sendMessage(TCPMessage.newInstance(result));
					}

					logger.info("进入桌子请求处理完成, userId: {}, tableId: {}, success: {}", clientId, mapId, result);
				}
			});

		} catch (Exception e) {
			logger.error("处理进入桌子请求失败, userId: {}", mapId, e);
		}
		return true;
	}

	/**
	 * 处理进入桌子逻辑
	 */
	private int processEnterTable(int userId, long tableId, int gateId, GameProto.ReqEnterTable req, Table table) {
		try {
			if (table.gaming()) {
				logger.error("桌子已经开始, userId: {}, tableId: {}", userId, tableId);
				return ConstProto.Result.TABLE_START_VALUE;
			}
			// 获取用户对象
			TableUser user = table.getUser(userId, gateId, req);

			// 尝试加入桌子
			int result = table.addUser(user);
			if (result == ConstProto.Result.SUCCESS_VALUE) {
				logger.debug("用户成功加入桌子, userId: {}, tableId: {}", userId, tableId);
				user.addTable(tableId);
			} else {
				logger.error("用户加入桌子失败, userId: {}, tableId: {}", userId, tableId);
			}

			return ConstProto.Result.SUCCESS_VALUE;
		} catch (Exception e) {
			logger.error("处理进入桌子逻辑失败, userId: {}, tableId: {}", userId, tableId, e);
			return ConstProto.Result.TABLE_ERROR_VALUE;
		}
	}

	/**
	 * 构建进入桌子响应
	 */
	private GameProto.AckEnterTable buildEnterTableResponse(Table table) {
		GameProto.AckEnterTable.Builder response = GameProto.AckEnterTable.newBuilder();
		response.setTableInfo(GameProto.TableInfo.newBuilder()
				.setTableId(table.getTableId())
				.setRoomId(table.getRoomId())
				.build());
		Map<Integer, TableUser> users = table.getUsers();
		for (Map.Entry<Integer, TableUser> entry : users.entrySet()) {
			TableUser tableUser = entry.getValue();
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