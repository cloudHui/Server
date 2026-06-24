package robot.connect.handle.room;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import msg.registor.message.GMsg;
import net.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GameProto;
import proto.RoomProto;
import robot.connect.ConnectProcessor;
import robot.game.RobotGameSession;
import robot.game.handler.RobotSessionHolder;
import utils.manager.ConnectHandle;
import utils.manager.HandleManager;

/**
 * 加入room房间成功
 * 记录桌子ID，自动发送进入桌子请求
 */
@ProcessClass(RoomProto.AckJoinRoomTable.class)
public class AckJoinRoomTableHandler implements ConnectHandle {
	private static final Logger logger = LoggerFactory.getLogger(AckJoinRoomTableHandler.class);

	@Override
	public void handle(Message message, Sender handler, int sequence, int transId) {
		if (message instanceof RoomProto.AckJoinRoomTable) {
			RoomProto.AckJoinRoomTable rooms = (RoomProto.AckJoinRoomTable) message;
			logger.info("[Robot] AckJoinRoomTable: tableId={}, 准备进入桌子", rooms.getTableId());

			RobotGameSession session = RobotSessionHolder.getSession();
			session.setTableId(rooms.getTableId());

			GameProto.ReqEnterTable.Builder builder = GameProto.ReqEnterTable.newBuilder();
			builder.setTableId(rooms.getTableId());
			HandleManager.sendMsg(GMsg.REQ_ENTER_TABLE_MSG, builder.build(), handler, ConnectProcessor.PARSER);
		}
	}
}
