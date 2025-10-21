package hall.connect.center.notice;

import com.google.protobuf.Message;
import hall.Hall;
import hall.connect.ConnectProcessor;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;
import proto.ServerProto;

/**
 * 注册信息通知
 */
@ProcessType(CMsg.REGISTER_NOTICE)
public class RegisterNoticeHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int aLong, Message msg, long mapId, int sequence) {
		Hall.getInstance().execute(() -> Hall.getInstance().getServerManager().connectToSever(
				((ServerProto.NotRegisterInfo) msg).getServersList(),
				Hall.getInstance().getServerId(), Hall.getInstance().getServerInfo().getIpConfig().toStringUtf8(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Hall));
		return true;
	}
}
