package game.handel;

import net.client.Sender;
import net.handler.Handler;
import proto.GameProto;

/**
 * 通知玩家掉线
 */
public class ReqEnterTableHandler implements Handler<GameProto.ReqEnterTable> {

	private static ReqEnterTableHandler instance = new ReqEnterTableHandler();

	public static ReqEnterTableHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, GameProto.ReqEnterTable req, int mapId) {

		return true;
	}
}
