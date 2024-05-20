package game.handel.client;

import msg.Message;
import net.client.Sender;
import net.handler.Handler;
import proto.GameProto;

/**
 * 通知玩家掉线
 */
public class ReqEnterTableHandler implements Handler<GameProto.ReqEnterTable> {

	private static final ReqEnterTableHandler instance = new ReqEnterTableHandler();

	public static ReqEnterTableHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, GameProto.ReqEnterTable req, int mapId) {
		GameProto.AckEnterTable.Builder ack = GameProto.AckEnterTable.newBuilder();
		ack.setTableId(1);
		sender.sendMessage(Message.GameMsg.ACK_ENTER_TABLE.getId(), ack.build(), null, mapId);
		return true;
	}
}
