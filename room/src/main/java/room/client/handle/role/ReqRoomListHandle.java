package room.client.handle.role;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.RMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.RoomProto;
import room.manager.table.TableManager;

/**
 * 处理客户端请求房间列表
 */
@ProcessType(RMsg.REQ_ROOM_LIST_MSG)
public class ReqRoomListHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqRoomListHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, long mapId, int sequence) {
		try {
			logger.debug("处理房间列表请求, clientId: {}", clientId);

			RoomProto.AckRoomList response = TableManager.getInstance().getAllRoomTable();
			sender.sendMessage(RMsg.ACK_ROOM_LIST_MSG, response, sequence);

			logger.info("返回房间列表, clientId: {}, 房间数: {}", clientId, response.getRoomListCount());
		} catch (Exception e) {
			logger.error("处理房间列表请求失败, clientId: {}", clientId, e);
		}
		return true;
	}
}
