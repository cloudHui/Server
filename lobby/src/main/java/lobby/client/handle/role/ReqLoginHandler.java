package lobby.client.handle.role;

import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import lobby.Lobby;
import lobby.db.UserEntity;
import lobby.db.UserRepository;
import lobby.manager.User;
import lobby.manager.UserManager;
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

			logger.info("登录请求, gateId: {}, username: {}, hasToken: {}",
					clientId, username, !token.isEmpty());

			UserEntity entity = null;
			UserRepository repo = Lobby.getInstance().getUserRepository();

			if (!token.isEmpty()) {
				entity = repo.findByToken(token).orElse(null);
				if (entity == null || !entity.isEnabled()) {
					sendAck(sender, clientId, sequence, CODE_FAIL, 0, "", "");
					return true;
				}
			} else if (!username.isEmpty() && !password.isEmpty()) {
				entity = repo.findByUsername(username).orElse(null);
				String hash = MD5Utils.MD5(password);
				if (entity == null || !entity.isEnabled()
						|| entity.getPasswordHash() == null
						|| !entity.getPasswordHash().equals(hash)) {
					sendAck(sender, clientId, sequence, CODE_FAIL, 0, "", "");
					return true;
				}
			} else {
				sendAck(sender, clientId, sequence, CODE_FAIL, 0, "", "");
				return true;
			}

			String newToken = Lobby.newToken();
			repo.updateLogin(entity.getId(), newToken, System.currentTimeMillis());

			User user = UserManager.getInstance().getUser(entity.getId());
			int gateConnId = clientId;
			if (sender instanceof net.client.handler.ClientHandler) {
				gateConnId = ((net.client.handler.ClientHandler) sender).getId();
			}
			if (user == null) {
				user = new User(entity.getId(), entity.getUsername(), entity.getNickname(), gateConnId);
				UserManager.getInstance().putOrUpdate(user);
			} else {
				user.setGateId(gateConnId);
				user.setNick(entity.getNickname());
				user.setUsername(entity.getUsername());
				user.updateActiveTime();
			}
			user.setPendingToken(newToken);

			TraceContext.setUserId((int) user.getUserId());
			List<Long> tables = user.getAllTables();
			sendAck(sender, clientId, sequence, CODE_OK, user.getUserIdInt(),
					user.getNick(), newToken, tables);
			logger.info("登录成功, userId: {}, tables: {}", user.getUserId(), tables.size());
		} catch (Exception e) {
			logger.error("处理登录失败, gateId: {}", clientId, e);
			sendAck(sender, clientId, sequence, CODE_FAIL, 0, "", "");
		}
		return true;
	}

	private void sendAck(Sender sender, int clientId, int sequence, int code,
						 int userId, String nick, String token) {
		sendAck(sender, clientId, sequence, code, userId, nick, token, null);
	}

	private void sendAck(Sender sender, int clientId, int sequence, int code,
						 int userId, String nick, String token, List<Long> tables) {
		try {
			LobbyProto.AckLogin.Builder builder = LobbyProto.AckLogin.newBuilder()
					.setCode(code)
					.setUserId(userId)
					.setNickName(ByteString.copyFromUtf8(nick == null ? "" : nick))
					.setToken(ByteString.copyFromUtf8(token == null ? "" : token));
			if (tables != null) {
				builder.addAllTables(tables);
			}
			sender.sendMessage(clientId, LMsg.ACK_LOGIN_MSG, 0, builder.build(), sequence);
		} catch (Exception e) {
			logger.error("发送 AckLogin 失败", e);
		}
	}
}
