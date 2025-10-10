package room.connect.handle.game;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.GMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.GameProto;

/**
 * 创建桌子回复
 */
@ProcessType(GMsg.ACK_CREATE_TABLE_MSG)
public class AckCreateTableHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		GameProto.AckCreateTable ack = (GameProto.AckCreateTable) msg;

		return true;
	}
}
