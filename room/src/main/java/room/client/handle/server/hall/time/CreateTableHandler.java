package room.client.handle.server.hall.time;


import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import net.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ServerProto;
import room.client.handle.role.ReqJoinTableHandle;
import room.manager.table.TableInfo;
import room.manager.table.TableManager;
import room.manager.user.User;
import room.manager.user.UserManager;
import utils.manager.ConnectHandle;

/**
 * 创建桌子响应处理器
 */
@ProcessClass(ServerProto.AckCreateGameTable.class)
public class CreateTableHandler implements ConnectHandle {
	private static final Logger logger = LoggerFactory.getLogger(CreateTableHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {

		// 处理创建桌子成功的逻辑
		ServerProto.AckCreateGameTable ackMessage = (ServerProto.AckCreateGameTable) message;

		// 真实用户的处理逻辑
		dealCreateSuccessTableJoin(sequence, ackMessage, transId);

		logger.info("创建桌子成功处理完成,userId: {}", transId);
	}

	/**
	 * 发送加入桌子成功的响应给客户端
	 */
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