package lobby.client.handle.server.game;

import com.google.protobuf.Message;
import lobby.client.handle.role.ReqJoinTableHandle;
import lobby.manager.User;
import lobby.manager.UserManager;
import lobby.manager.table.TableInfo;
import lobby.manager.table.TableManager;
import msg.annotation.ProcessClass;
import net.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;
import utils.manager.ConnectHandle;

@ProcessClass(ServerProto.AckCreateGameTable.class)
public class CreateTableHandler implements ConnectHandle {
	private static final Logger logger = LoggerFactory.getLogger(CreateTableHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		ServerProto.AckCreateGameTable ackMessage = (ServerProto.AckCreateGameTable) message;
		dealCreateSuccessTableJoin(sequence, ackMessage, transId);
		logger.info("创建桌子成功处理完成,userId: {}", transId);
	}

	private void dealCreateSuccessTableJoin(int sequence, ServerProto.AckCreateGameTable ack, int userId) {
		TableInfo tableInfo = TableManager.getInstance().putRoomInfo(ack.getTables());
		if (tableInfo == null) {
			logger.warn("创建桌子信息注册失败, userId: {}", userId);
			return;
		}
		User user = UserManager.getInstance().getUser(userId);
		if (user == null) {
			logger.warn("创建桌子时用户不存在, userId: {}", userId);
			return;
		}
		tableInfo.joinRole(user);
		ReqJoinTableHandle.sendJoinTableAck(ack.getTables().getTableId(), sequence, user);
	}
}
