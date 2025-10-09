package room.connect.handle;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 通知玩家掉线
 */
@ProcessType(value = CMsg.NOT_BREAK, trans = ModelProto.NotBreak.class)
public class NotBreakHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		ModelProto.NotBreak notice = (ModelProto.NotBreak) msg;
		return true;
	}
}
