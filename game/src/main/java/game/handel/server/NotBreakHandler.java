package game.handel.server;

import com.google.protobuf.Message;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 通知玩家掉线
 */
public class NotBreakHandler implements Handler {

	private static final NotBreakHandler instance = new NotBreakHandler();

	public static NotBreakHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, long roleId, Message msg, int mapId) {
		ModelProto.NotBreak req = (ModelProto.NotBreak) msg;
		return true;
	}
}
