package lobby.client.handle.server.gate;

import com.google.protobuf.Message;
import lobby.manager.User;
import lobby.manager.UserManager;
import msg.annotation.ProcessType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;

/**
 * 玩家断线：仅标记 offline，保留 User 与 tables，便于重登拉回桌子。
 * 正式离桌走 leave；被顶号后的旧连接 NotBreak 用 gateClientId 忽略。
 */
@ProcessType(CMsg.NOT_BREAK)
public class NotBreakHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(NotBreakHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, long mapId, int sequence) {
		try {
			ServerProto.NotBreak notice = (ServerProto.NotBreak) msg;
			int userId = notice.getUserId();
			int gateClientId = notice.getGateClientId();
			logger.info("收到玩家断线通知, userId: {}, gateClientId: {}", userId, gateClientId);
			handleUserDisconnect(userId, gateClientId);
			return true;
		} catch (Exception e) {
			logger.error("处理断线通知失败, clientId: {}", clientId, e);
			return false;
		}
	}

	private void handleUserDisconnect(int userId, int gateClientId) {
		User user = UserManager.getInstance().getUser(userId);
		if (user == null) {
			logger.warn("用户不存在,无法处理断线, userId: {}", userId);
			return;
		}
		// 已被新连接顶替：旧连接断线忽略，避免把新会话标 offline / 清桌
		if (gateClientId != 0 && user.getGateId() != 0 && user.getGateId() != gateClientId) {
			logger.info("忽略旧连接断线, userId: {}, noticeGate: {}, currentGate: {}",
					userId, gateClientId, user.getGateId());
			return;
		}
		user.setOffline(true);
		logger.info("玩家标记离线(保留桌子), userId: {}, tables: {}", userId, user.getAllTables().size());
	}
}
