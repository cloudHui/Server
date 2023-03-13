package game.handel;

import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 通知玩家掉线
 */
public class NotBreakHandler implements Handler<ModelProto.NotBreak> {

	private static NotBreakHandler instance = new NotBreakHandler();

	public static NotBreakHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, ModelProto.NotBreak req) {

		return true;
	}
}
