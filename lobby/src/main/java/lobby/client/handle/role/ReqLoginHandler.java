package lobby.client.handle.role;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import lobby.Lobby;
import lobby.db.UserEntity;
import lobby.db.UserRepository;
import lobby.manager.User;
import lobby.manager.UserManager;
import lobby.manager.table.TableInfo;
import lobby.manager.table.TableManager;
import msg.annotation.ProcessType;
import msg.registor.message.LMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.LobbyProto;
import utils.other.MD5Utils;
import utils.trace.TraceContext;

/**
 * 登录：username+password 或 token 重连；本地填 tables，立即 AckLogin
 */
@ProcessType(LMsg.REQ_LOGIN_MSG)
public class ReqLoginHandler implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqLoginHandler.class);

	public static final int CODE_OK = 0;
	public static final int CODE_FAIL = 1;

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			LobbyProto.ReqLogin request = (LobbyProto.ReqLogin) message;
			String username = request.getUsername().toStringUtf8();
			String password = request.getPassword().toStringUtf8();
			String token = request.getToken().toStringUtf8();

			logger.info("登录请求, gateClientId: {}, username: {}, hasToken: {}",
					clientId, username, !token.isEmpty());

			UserEntity entity = null;
			UserRepository repo = Lobby.getInstance().getUserRepository();

			if (!token.isEmpty()) {
				entity = repo.findByToken(token).orElse(null);
				if (entity == null || !entity.isEnabled()) {
					sendAck(sender, clientId, sequence, CODE_FAIL, 0, "", "", "", null);
					return true;
				}
			} else if (!username.isEmpty() && !password.isEmpty()) {
				entity = repo.findByUsername(username).orElse(null);
				String hash = MD5Utils.MD5(password);
				if (entity == null || !entity.isEnabled()
						|| entity.getPasswordHash() == null
						|| !entity.getPasswordHash().equals(hash)) {
					sendAck(sender, clientId, sequence, CODE_FAIL, 0, "", "", "", null);
					return true;
				}
			} else {
				sendAck(sender, clientId, sequence, CODE_FAIL, 0, "", "", "", null);
				return true;
			}

			String newToken = Lobby.newToken();
			repo.updateLogin(entity.getId(), newToken, System.currentTimeMillis());

			// clientId = gate 上玩家连接 id（用于顶号后忽略旧 NotBreak）
			int gateConnId = clientId;
			User user = UserManager.getInstance().getUser(entity.getId());
			if (user == null) {
				user = new User(entity.getId(), entity.getUsername(), entity.getNickname(), gateConnId);
				UserManager.getInstance().putOrUpdate(user);
			} else {
				user.setGateId(gateConnId);
				user.setNick(entity.getNickname());
				user.setUsername(entity.getUsername());
				user.updateActiveTime();
			}
			user.setOffline(false);
			user.setPendingToken(newToken);

			TraceContext.setUserId((int) user.getUserId());
			List<Long> tables = user.getAllTables();
			List<LobbyProto.TableSeatInfo> tableInfos = buildTableInfos(tables);
			sendAck(sender, clientId, sequence, CODE_OK, user.getUserIdInt(),
					entity.getUsername(), user.getNick(), newToken, tables, tableInfos);
			logger.info("登录成功, userId: {}, username: {}, tables: {}",
					user.getUserId(), entity.getUsername(), tables.size());
		} catch (Exception e) {
			logger.error("处理登录失败, gateClientId: {}", clientId, e);
			sendAck(sender, clientId, sequence, CODE_FAIL, 0, "", "", "", null);
		}
		return true;
	}

	static List<LobbyProto.TableSeatInfo> buildTableInfos(List<Long> tables) {
		List<LobbyProto.TableSeatInfo> list = new ArrayList<>();
		if (tables == null) {
			return list;
		}
		for (Long tableId : tables) {
			if (tableId == null) {
				continue;
			}
			LobbyProto.TableSeatInfo.Builder b = LobbyProto.TableSeatInfo.newBuilder()
					.setTableId(tableId);
			TableInfo info = TableManager.getInstance().getTableById(tableId);
			if (info != null && info.getModel() != null) {
				b.setRoomId(info.getModel().getId());
				b.setGameType(info.getModel().getType());
			}
			list.add(b.build());
		}
		return list;
	}

	private void sendAck(Sender sender, int clientId, int sequence, int code,
						 int userId, String username, String nick, String token,
						 List<Long> tables) {
		sendAck(sender, clientId, sequence, code, userId, username, nick, token, tables, null);
	}

	private void sendAck(Sender sender, int clientId, int sequence, int code,
						 int userId, String username, String nick, String token,
						 List<Long> tables, List<LobbyProto.TableSeatInfo> tableInfos) {
		try {
			LobbyProto.AckLogin.Builder builder = LobbyProto.AckLogin.newBuilder()
					.setCode(code)
					.setUserId(userId)
					.setNickName(ByteString.copyFromUtf8(nick == null ? "" : nick))
					.setToken(ByteString.copyFromUtf8(token == null ? "" : token))
					.setUsername(ByteString.copyFromUtf8(username == null ? "" : username));
			if (tables != null) {
				builder.addAllTables(tables);
			}
			if (tableInfos != null) {
				builder.addAllTableInfos(tableInfos);
			}
			sender.sendMessage(clientId, LMsg.ACK_LOGIN_MSG, 0, builder.build(), sequence);
		} catch (Exception e) {
			logger.error("发送 AckLogin 失败", e);
		}
	}
}
