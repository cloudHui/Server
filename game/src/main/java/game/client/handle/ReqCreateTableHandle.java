package game.client.handle;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ServerProto;

/**
 * 房间服务请求创建桌子
 */
@ProcessType(SMsg.REQ_CREATE_TABLE_MSG)
public class ReqCreateTableHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		ServerProto.ReqCreateGameTable req = (ServerProto.ReqCreateGameTable) msg;
		ServerProto.AckCreateGameTable.Builder ack = ServerProto.AckCreateGameTable.newBuilder();
		ack.setRoomId(req.getRoomId());
		sender.sendMessage(clientId, SMsg.ACK_CREATE_TABLE_MSG, mapId, ack.build(), sequence);
		return true;
	}
}
