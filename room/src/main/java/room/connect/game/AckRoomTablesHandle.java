package room.connect.game;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;
import room.manager.table.TableManager;

@ProcessType(SMsg.ACK_ROOM_TABLES_MSG)
public class AckRoomTablesHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(AckRoomTablesHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			ServerProto.AckRoomTables ack = (ServerProto.AckRoomTables) message;
			TableManager.getInstance().restoreTables(ack.getTablesList());
			logger.info("收到Game桌子列表恢复, count: {}", ack.getTablesCount());
		} catch (Exception e) {
			logger.error("处理Game桌子列表失败", e);
		}
		return true;
	}
}
