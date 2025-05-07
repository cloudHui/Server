package room.connect.handle;

import com.google.protobuf.Message;
import msg.registor.message.CMsg;
import msg.registor.enums.ServerType;
import msg.annotation.ProcessType;
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
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		Room.getInstance().execute(()-> Room.getInstance().getServerManager().connectToSever(
				((ModelProto.NotRegisterInfo) msg).getServersList(),
				Room.getInstance().getServerId(), Room.getInstance().getServerInfo().getIpConfig().toStringUtf8(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Room));

		return true;
	}
}
