package game.client.handle;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.GMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.GameProto;

/**
 * 请求创建桌子
 */
@ProcessType(GMsg.REQ_CREATE_TABLE_MSG)
public class ReqCreateTableHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		GameProto.ReqCreateTable req = (GameProto.ReqCreateTable) msg;
		GameProto.AckCreateTable.Builder ack = GameProto.AckCreateTable.newBuilder();
		ack.setTableId(1);
		sender.sendMessage(clientId, GMsg.ACK_CREATE_TABLE_MSG, mapId, 0, ack.build(), sequence);
		return true;
	}
}
