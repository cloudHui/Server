package gate.handel.server;

import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 注册信息通知
 */
public class AckServerInfoHandel implements Handler<ModelProto.AckServerInfo> {

	private static AckServerInfoHandel instance = new AckServerInfoHandel();

	public static AckServerInfoHandel getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, Long aLong, ModelProto.AckServerInfo req) {
		return RegisterNoticeHandler.connectToSever(req.getServersList());
	}
}
