package room.client.handle;

import com.google.protobuf.Message;
import model.TableModel;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.RMsg;
import msg.registor.message.SMsg;
import net.client.Sender;
import net.connect.handle.ConnectHandler;
import net.handler.Handler;
import net.message.TCPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ResultProto;
import proto.RoomProto;
import proto.ServerProto;
import room.Room;
import room.manager.RoomManager;

/**
 * 玩家请求进入桌子
 */
@ProcessType(RMsg.REQ_JOIN_ROOM_TABLE_MSG)
public class ReqJoinTableHandle implements Handler {

	private final static Logger LOGGER = LoggerFactory.getLogger(ReqJoinTableHandle.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		RoomProto.ReqJoinRoomTable req = (RoomProto.ReqJoinRoomTable) msg;
		int roomId = req.getRoomId();

		TableModel tableModel = RoomManager.getInstance().getTableModel(roomId);
		if (tableModel == null) {
			LOGGER.error("roomId:{} config null", roomId);
			return true;
		}

		ConnectHandler game = Room.getInstance().getServerManager().getServerClient(ServerType.Game);

		if (game == null) {
			sender.sendMessage(TCPMessage.newInstance(ResultProto.Result.SERVER_NULL_VALUE));
		} else {
			//向发送创建房间消息
			game.sendMessage(ServerProto.ReqCreateGameTable.newBuilder()
					.setRoomId(roomId)
					.setRoomRole(ServerProto.RoomTableRole.newBuilder()
							//Todo 加玩家信息
							//.set
							.build())
					.build(), SMsg.REQ_CREATE_TABLE_MSG, 3)
					.whenComplete((ack, throwable) -> {
						if (throwable != null) {
							LOGGER.error("[向game 发送创建房间失败] {}", throwable.getMessage());
						} else {
							sender.sendMessage(clientId, RMsg.ACK_JOIN_ROOM_TABLE_MSG, mapId, RoomProto.AckJoinRoomTable.newBuilder()
									//Todo 添加房间信息 ack
									.build(), sequence);
						}
					});
		}
		return true;
	}
}
