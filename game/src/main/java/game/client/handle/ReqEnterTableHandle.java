package game.client.handle;

import com.google.protobuf.Message;
import msg.registor.message.GMsg;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.GameProto;

/**
 * 请求加入桌子
 */
@ProcessType(GMsg.REQ_ENTER_TABLE_MSG)
public class ReqEnterTableHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		GameProto.ReqEnterTable req = (GameProto.ReqEnterTable) msg;
		GameProto.AckEnterTable.Builder ack = GameProto.AckEnterTable.newBuilder();
		ack.setTableId(1);
		sender.sendMessage(clientId, GMsg.ACK_ENTER_TABLE_MSG, mapId, 0, ack.build(), sequence);
		return true;
	}
}
