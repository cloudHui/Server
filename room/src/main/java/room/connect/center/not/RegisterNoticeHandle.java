package room.connect.center.not;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;
import room.Room;
import room.connect.ConnectProcessor;

/**
 * 注册信息通知
 */
@ProcessType(CMsg.REGISTER_NOTICE)
public class RegisterNoticeHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, int sequence) {
		Room.getInstance().execute(() -> Room.getInstance().getServerManager().connectToSever(
				((ModelProto.NotRegisterInfo) msg).getServersList(),
				Room.getInstance().getServerId(), Room.getInstance().getServerInfo().getIpConfig().toStringUtf8(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Room));

		return true;
	}
}
