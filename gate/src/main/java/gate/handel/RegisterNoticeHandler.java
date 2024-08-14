package gate.handel;

import com.google.protobuf.Message;
import gate.Gate;
import gate.connect.ConnectProcessor;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 注册信息通知
 */
public class RegisterNoticeHandler implements Handler {

	private static final RegisterNoticeHandler instance = new RegisterNoticeHandler();

	public static RegisterNoticeHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, long aLong, Message registerInfo, int mapId) {
		ModelProto.NotRegisterInfo req = (ModelProto.NotRegisterInfo) registerInfo;

		return Gate.getInstance().getServerManager().connectToSever(req.getServersList(),
				Gate.getInstance().getServerId(), Gate.getInstance().getInnerIp() + "：" + Gate.getInstance().getPort(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS);
	}
}
