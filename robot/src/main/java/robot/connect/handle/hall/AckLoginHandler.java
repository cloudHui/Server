package robot.connect.handle.hall;

import com.google.protobuf.Message;
import msg.annotation.ProcessClass;
import msg.registor.message.RMsg;
import net.connect.handle.ConnectHandler;
import proto.HallProto;
import proto.RoomProto;
import robot.connect.ConnectProcessor;
import utils.manager.ConnectHandle;
import utils.manager.HandleManager;

/**
 * 登录回复
 */
@ProcessClass(HallProto.AckLogin.class)
public class AckLoginHandler implements ConnectHandle {

	@Override
	public void handle(Message message, ConnectHandler serverClient, int sequence) {
		if (message instanceof HallProto.AckLogin) {
			HallProto.AckLogin ack = (HallProto.AckLogin) message;
			LOGGER.error("AckLogin:{}", ack.toString());
			RoomProto.ReqGetRoomList.Builder builder = RoomProto.ReqGetRoomList.newBuilder();
			HandleManager.sendMsg(RMsg.REQ_ROOM_LIST_MSG, builder.build(), serverClient, ConnectProcessor.PARSER);
		}
	}
}
