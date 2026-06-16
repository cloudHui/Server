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

@ProcessType(SMsg.NOT_TABLE_DESTROYED_MSG)
public class NotTableDestroyedHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(NotTableDestroyedHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			ServerProto.NotTableDestroyed not = (ServerProto.NotTableDestroyed) message;
			long tableId = not.getTableId();
			TableManager.getInstance().removeTable(tableId);
			logger.info("收到桌子销毁通知, tableId: {}", tableId);
		} catch (Exception e) {
			logger.error("处理桌子销毁通知失败", e);
		}
		return true;
	}
}
