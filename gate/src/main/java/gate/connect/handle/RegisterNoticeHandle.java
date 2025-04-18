package gate.connect.handle;

import com.google.protobuf.Message;
import gate.Gate;
import gate.connect.ConnectProcessor;
import msg.MessageId;
import msg.ServerType;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 注册信息通知
 */
@ProcessType(MessageId.REGISTER_NOTICE)
public class RegisterNoticeHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message registerInfo, int mapId, long sequence) {
		Gate.getInstance().execute(() -> Gate.getInstance().getServerManager().connectToSever(
				((ModelProto.NotRegisterInfo) registerInfo).getServersList(), Gate.getInstance().getServerId(),
				(Gate.getInstance().getInnerIp() + "：" + Gate.getInstance().getPort()),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Gate));
		return true;
	}
}
