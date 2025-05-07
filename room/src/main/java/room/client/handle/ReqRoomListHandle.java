package room.client.handle;

import java.util.List;

import com.google.protobuf.Message;
import model.TableModel;
import msg.registor.message.RMsg;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.RoomProto;
import room.manager.RoomModelManager;

/**
 * 请求房间列表
 */
@ProcessType(RMsg.REQ_ROOM_LIST_MSG)
public class ReqRoomListHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
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
		sender.sendMessage(clientId, RMsg.ACK_ROOM_LIST_MSG, mapId, 0, ack.build(), sequence);
		return true;
	}
}
