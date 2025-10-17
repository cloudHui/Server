package robot.connect.handle.room;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import msg.registor.message.GMsg;
import net.connect.handle.ConnectHandler;
import proto.GameProto;
import proto.RoomProto;
import robot.connect.ConnectProcessor;
import utils.manager.ConnectHandle;
import utils.manager.HandleManager;

/**
 * 加入room房间成功
 */
@ProcessClass(RoomProto.AckJoinRoomTable.class)
public class AckJoinRoomTableHandler implements ConnectHandle {

	@Override
	public void handle(Message message, ConnectHandler handler, int sequence, int transId) {
		if (message instanceof RoomProto.AckJoinRoomTable) {
			RoomProto.AckJoinRoomTable rooms = (RoomProto.AckJoinRoomTable) message;
			LOGGER.error("AckJoinRoomTable:{}", rooms.toString());
			String tableId = rooms.getTableId().toStringUtf8();
			GameProto.ReqEnterTable.Builder builder = GameProto.ReqEnterTable.newBuilder();
			builder.setTableId(ByteString.copyFromUtf8(tableId));
			HandleManager.sendMsg(GMsg.REQ_ENTER_TABLE_MSG, builder.build(), handler, ConnectProcessor.PARSER);
		}
	}
}
