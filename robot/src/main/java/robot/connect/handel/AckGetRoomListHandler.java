package robot.connect.handel;

import com.google.protobuf.Message;
import msg.RoomMessageId;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.RoomProto;

/**
 * 获取房间回复
 */
@ProcessType(RoomMessageId.ACK_ROOM_LIST_MSG)
public class AckGetRoomListHandler implements Handler {

	private final static Logger logger = LoggerFactory.getLogger(AckGetRoomListHandler.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message ack, int mapId, long sequence) {
		logger.error("[get room ack:{}]", ack.toString());
		RoomProto.AckGetRoomList rooms = (RoomProto.AckGetRoomList) ack;
		if (rooms.getRoomListCount() > 0) {
			RoomProto.Room room = rooms.getRoomList(0);
			if (room.getTablesCount() == 0) {
				//creatTable();
			} else {
				RoomProto.Room.Table sitTable = null;
				for (RoomProto.Room.Table table : room.getTablesList()) {
					if (!table.getFull()) {
						sitTable = table;
						break;
					}
				}
				if (sitTable != null) {
					//joinTable();
				} else {
					//creatTable();
				}
			}
		}
		return true;
	}
}
