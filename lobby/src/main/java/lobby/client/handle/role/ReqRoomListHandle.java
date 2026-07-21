package lobby.client.handle.role;

import com.google.protobuf.Message;
import lobby.manager.table.TableManager;
import msg.annotation.ProcessType;
import msg.registor.message.LMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.LobbyProto;

@ProcessType(LMsg.REQ_ROOM_LIST_MSG)
public class ReqRoomListHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqRoomListHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, long mapId, int sequence) {
		try {
			LobbyProto.AckRoomList response = TableManager.getInstance().getAllRoomTable();
			sender.sendMessage(LMsg.ACK_ROOM_LIST_MSG, response, sequence);
			logger.info("返回房间列表, clientId: {}, 房间数: {}", clientId, response.getRoomListCount());
		} catch (Exception e) {
			logger.error("处理房间列表请求失败, clientId: {}", clientId, e);
		}
		return true;
	}
}
