package center.client.handle;

import annotation.ProcessType;
import com.google.protobuf.Message;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * gate通知玩家掉线
 */
@ProcessType(value = CMsg.NOT_BREAK)
public class NotBreakHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		ModelProto.NotBreak req = (ModelProto.NotBreak) msg;
		NotClientLinkHandle.clientDisconnect(req.getCert().toStringUtf8());
		return true;
	}
}