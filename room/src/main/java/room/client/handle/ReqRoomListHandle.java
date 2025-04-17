package room.client.handle;

import java.util.List;

import com.google.protobuf.Message;
import model.TableModel;
import msg.RoomMessageId;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.RoomProto;
import room.manager.RoomModelManager;

/**
 * 请求房间列表
 */
@ProcessType(RoomMessageId.REQ_ROOM_LIST_MSG)
public class ReqRoomListHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int aLong, Message msg, int mapId, long sequence) {
		RoomProto.AckGetRoomList.Builder ack = RoomProto.AckGetRoomList.newBuilder();
		List<TableModel> models = RoomModelManager.getInstance().getModels();
		if (!models.isEmpty()) {
			RoomProto.Room.Builder room = RoomProto.Room.newBuilder();
			for (TableModel model : models) {
				room.setRoomId(model.getId());
				room.setConfigTypeId(model.getType());
				ack.addRoomList(room);
			}
		}
		sender.sendMessage(RoomMessageId.ACK_ROOM_LIST_MSG, ack.build(), mapId, sequence);
		return true;
	}
}
