package game.handle.client;

import com.google.protobuf.Message;
import game.msg.GameMessageId;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.GameProto;

/**
 * 请求加入桌子
 */
@ProcessType(GameMessageId.REQ_ENTER_TABLE_MSG)
public class ReqEnterTableHandler implements Handler {

	@Override
	public boolean handler(Sender sender, int roleId, Message msg, int mapId, long sequence) {
		GameProto.ReqEnterTable req = (GameProto.ReqEnterTable) msg;
		GameProto.AckEnterTable.Builder ack = GameProto.AckEnterTable.newBuilder();
		ack.setTableId(1);
		sender.sendMessage(GameMessageId.ACK_ENTER_TABLE_MSG, ack.build(), mapId, sequence);
		return true;
	}
}
