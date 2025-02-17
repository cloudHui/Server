package gate.handle;

import com.google.protobuf.Message;
import gate.Gate;
import gate.connect.ConnectProcessor;
import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 注册信息通知
 */
public class AckServerInfoHandel implements Handler {

	private static final AckServerInfoHandel instance = new AckServerInfoHandel();

	public static AckServerInfoHandel getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, int aLong, Message ackServerInfo, int mapId, long sequence) {

		ModelProto.AckServerInfo req = (ModelProto.AckServerInfo) ackServerInfo;
		Gate.getInstance().getServerManager().connectToSever(req.getServersList(),
				Gate.getInstance().getServerId(),
				Gate.getInstance().getInnerIp() + "：" + Gate.getInstance().getPort(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Gate);
		return true;
	}
}
