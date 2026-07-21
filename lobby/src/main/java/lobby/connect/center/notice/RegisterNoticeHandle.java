package lobby.connect.center.notice;

import com.google.protobuf.Message;
import lobby.Lobby;
import lobby.connect.ConnectProcessor;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ServerProto;

@ProcessType(CMsg.REGISTER_NOTICE)
public class RegisterNoticeHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, long mapId, int sequence) {
		Lobby.getInstance().execute(() -> Lobby.getInstance().getServerManager().connectToSever(
				((ServerProto.NotRegisterInfo) msg).getServersList(),
				Lobby.getInstance().getServerId(),
				Lobby.getInstance().getServerInfo().getIpConfig().toStringUtf8(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Lobby));
		return true;
	}
}
