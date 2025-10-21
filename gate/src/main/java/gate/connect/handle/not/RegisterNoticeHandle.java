package gate.connect.handle.not;

import com.google.protobuf.Message;
import gate.Gate;
import gate.connect.ConnectProcessor;
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
	public boolean handler(Sender sender, int clientId, Message registerInfo, long mapId, int sequence) {
		Gate.getInstance().execute(() -> Gate.getInstance().getServerManager().connectToSever(
				((ServerProto.NotRegisterInfo) registerInfo).getServersList(), Gate.getInstance().getServerId(),
				(Gate.getInstance().getInnerIp() + "：" + Gate.getInstance().getPort()),
				null, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Gate));
		return true;
	}
}
