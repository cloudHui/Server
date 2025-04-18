package room.client.handle;

import com.google.protobuf.Message;
import model.TableModel;
import msg.RoomMessageId;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.RoomProto;
import room.Room;
import room.manager.RoomModelManager;

/**
 * 请求创建桌子
 */
@ProcessType(RoomMessageId.REQ_CREATE_TABLE_MSG)
public class ReqCreateTableHandle implements Handler {

	private final static Logger LOGGER = LoggerFactory.getLogger(ReqCreateTableHandle.class);

	@Override
	public boolean handler(Sender sender, int aLong, Message msg, int mapId, long sequence) {
		RoomProto.ReqCreateTable req = (RoomProto.ReqCreateTable) msg;
		int configTypeId = req.getConfigTypeId();

		TableModel tableModel = RoomModelManager.getInstance().getTableModel(configTypeId);
		if (tableModel == null) {
			LOGGER.error("");
			return true;
		}

		sender.sendMessage(RoomMessageId.ACK_ROOM_LIST_MSG, ack.build(), mapId, sequence);
		return true;
	}
}
