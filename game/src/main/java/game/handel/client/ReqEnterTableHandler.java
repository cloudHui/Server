package game.handel.client;

import com.google.protobuf.Message;
import msg.MessageId;
import net.client.Sender;
import net.handler.Handler;
import proto.GameProto;

/**
 * 通知玩家掉线
 */
public class ReqEnterTableHandler implements Handler {

	private static final ReqEnterTableHandler instance = new ReqEnterTableHandler();

	public static ReqEnterTableHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, int roleId, Message msg, int mapId, long sequence) {
		GameProto.ReqEnterTable req = (GameProto.ReqEnterTable) msg;
		GameProto.AckEnterTable.Builder ack = GameProto.AckEnterTable.newBuilder();
		ack.setTableId(1);
		sender.sendMessage(MessageId.GameMsg.ACK_ENTER_TABLE.getId(), ack.build(), null, mapId, sequence);
		return true;
	}
}
