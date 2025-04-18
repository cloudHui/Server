package game.client.handle;

import com.google.protobuf.Message;
import msg.MessageId;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * gate通知玩家掉线
 */
@ProcessType(value = MessageId.NOT_BREAK)
public class NotBreakHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		ModelProto.NotBreak req = (ModelProto.NotBreak) msg;
		return true;
	}
}
