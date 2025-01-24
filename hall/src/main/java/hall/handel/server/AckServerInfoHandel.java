package hall.handel.server;

import com.google.protobuf.Message;
import hall.Hall;
import hall.connect.ConnectProcessor;
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
	public boolean handler(Sender sender, int aLong, Message ackServerInfo, int mapId,long sequence) {

		ModelProto.AckServerInfo req = (ModelProto.AckServerInfo) ackServerInfo;
		return Hall.getInstance().getServerManager().connectToSever(req.getServersList(),
				Hall.getInstance().getServerId(),
				Hall.getInstance().getInnerIp() + "：" + Hall.getInstance().getPort(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Hall);
	}
}
