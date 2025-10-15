package room.client.handle.server.center;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;
import room.manager.User;
import room.manager.UserManager;

/**
 * 通知玩家掉线
 */
@ProcessType(CMsg.NOT_BREAK)
public class NotBreakHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		ModelProto.NotBreak notice = (ModelProto.NotBreak) msg;
		User user = UserManager.getInstance().getUser(notice.getUserId());
		if (user != null) {
			//Todo 设置离线状态
		}
		return true;
	}
}
