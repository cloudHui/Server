package game.handel.client;

import msg.MessageHandel;
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
		GameProto.AckEnterTable.Builder ack = GameProto.AckEnterTable.newBuilder();
		ack.setTableId(1);
		sender.sendMessage(MessageHandel.GameMsg.ENTER_TABLE_ACK.getId(), ack.build(), null, mapId);
		return true;
	}
}
