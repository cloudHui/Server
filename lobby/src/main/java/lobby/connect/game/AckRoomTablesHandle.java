package lobby.connect.game;

import com.google.protobuf.Message;
import lobby.manager.table.TableManager;
import msg.annotation.ProcessClass;
import msg.annotation.ProcessType;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;
import tools.manager.ConnectHandle;

@ProcessType(SMsg.ACK_ROOM_TABLES_MSG)
@ProcessClass(ServerProto.AckRoomTables.class)
public class AckRoomTablesHandle implements Handler, ConnectHandle {
	private static final Logger logger = LoggerFactory.getLogger(AckRoomTablesHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		restore(message);
		return true;
	}

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		restore(message);
	}

	private void restore(Message message) {
		try {
			ServerProto.AckRoomTables ack = (ServerProto.AckRoomTables) message;
			TableManager.getInstance().restoreTables(ack.getTablesList());
			logger.info("收到Game桌子列表恢复, count: {}", ack.getTablesCount());
		} catch (Exception e) {
			logger.error("处理Game桌子列表失败", e);
		}
	}
}
