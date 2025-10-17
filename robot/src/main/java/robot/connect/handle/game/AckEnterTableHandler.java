package robot.connect.handle.game;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import msg.registor.message.RMsg;
import net.connect.handle.ConnectHandler;
import proto.GameProto;
import proto.RoomProto;
import robot.connect.ConnectProcessor;
import utils.manager.ConnectHandle;
import utils.manager.HandleManager;

/**
 * 登录回复
 */
@ProcessClass(GameProto.AckEnterTable.class)
public class AckEnterTableHandler implements ConnectHandle {

	@Override
	public void handle(Message message, ConnectHandler handler, int sequence, int transId) {
		if (message instanceof GameProto.AckEnterTable) {
			GameProto.AckEnterTable ack = (GameProto.AckEnterTable) message;
			LOGGER.error("AckEnterTable:{}", ack.toString());
			RoomProto.ReqGetRoomList.Builder builder = RoomProto.ReqGetRoomList.newBuilder();
			HandleManager.sendMsg(RMsg.REQ_ROOM_LIST_MSG, builder.build(), handler, ConnectProcessor.PARSER);
		}
	}
}
