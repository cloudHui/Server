package robot.connect.handle.room;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import msg.registor.message.GMsg;
import net.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private static final Logger logger = LoggerFactory.getLogger(AckJoinRoomTableHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		if (message instanceof RoomProto.AckJoinRoomTable) {
			RoomProto.AckJoinRoomTable rooms = (RoomProto.AckJoinRoomTable) message;
			logger.error("AckJoinRoomTable:{}", rooms.toString());
			GameProto.ReqEnterTable.Builder builder = GameProto.ReqEnterTable.newBuilder();
			builder.setTableId(rooms.getTableId());
			HandleManager.sendMsg(GMsg.REQ_ENTER_TABLE_MSG, builder.build(), handler, ConnectProcessor.PARSER);
		}
	}
}
