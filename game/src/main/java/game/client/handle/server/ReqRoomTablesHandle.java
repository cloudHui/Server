package game.client.handle.server;

import com.google.protobuf.Message;
import game.Game;
import game.manager.TableManager;
import msg.annotation.ProcessType;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ModelProto;
import proto.ServerProto;

import java.util.List;

@ProcessType(SMsg.REQ_ROOM_TABLES_MSG)
public class ReqRoomTablesHandle implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqRoomTablesHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, long mapId, int sequence) {
		try {
			TableManager tableManager = Game.getInstance().getTableManager();
			tableManager.getAllTableInfoAsync().whenComplete((tables, error) -> {
				if (error != null) {
					logger.error("读取桌子列表失败, clientId: {}", clientId, error);
					return;
				}
				ServerProto.AckRoomTables ack = ServerProto.AckRoomTables.newBuilder()
						.addAllTables(tables).build();
				sender.sendMessage(clientId, SMsg.ACK_ROOM_TABLES_MSG, mapId, ack, sequence);
				logger.info("返回桌子列表给Lobby, count: {}, clientId: {}", tables.size(), clientId);
			});
		} catch (Exception e) {
			logger.error("处理Lobby桌子列表请求失败, clientId: {}", clientId, e);
		}
		return true;
	}
}
