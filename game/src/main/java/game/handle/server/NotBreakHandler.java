package game.handle.server;

import com.google.protobuf.Message;
import msg.MessageId;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 通知玩家掉线
 */
@ProcessType(MessageId.NOT_BREAK)
public class NotBreakHandler implements Handler {

	@Override
	public boolean handler(Sender sender, int roleId, Message msg, int mapId, long sequence) {
		ModelProto.NotBreak req = (ModelProto.NotBreak) msg;
		return true;
	}
}
