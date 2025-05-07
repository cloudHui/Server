package room.client.handle;

import com.google.protobuf.Message;
import model.TableModel;
import msg.registor.message.RMsg;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.RoomProto;
import room.manager.RoomModelManager;

/**
 * 请求创建桌子
 */
@ProcessType(RMsg.REQ_CREATE_ROOM_TABLE_MSG)
public class ReqCreateTableHandle implements Handler {

	private final static Logger LOGGER = LoggerFactory.getLogger(ReqCreateTableHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		RoomProto.ReqCreateRoomTable req = (RoomProto.ReqCreateRoomTable) msg;
		int configTypeId = req.getConfigTypeId();

		TableModel tableModel = RoomModelManager.getInstance().getTableModel(configTypeId);
		if (tableModel == null) {
			LOGGER.error("");
			return true;
		}
		sender.sendMessage(clientId, RMsg.ACK_ROOM_LIST_MSG, mapId, 0, req, sequence);
		return true;
	}
}
