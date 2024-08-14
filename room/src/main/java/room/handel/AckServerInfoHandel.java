package room.handel;

import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;
import room.Room;
import room.connect.ConnectProcessor;

/**
 * 注册信息通知
 */
public class AckServerInfoHandel implements Handler<ModelProto.AckServerInfo> {

	private static final AckServerInfoHandel instance = new AckServerInfoHandel();

	public static AckServerInfoHandel getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, long aLong, ModelProto.AckServerInfo req, int mapId) {
		return Room.getInstance().getServerManager().connectToSever(req.getServersList(),
				Room.getInstance().getServerId(),
				Room.getInstance().getInnerIp() + "：" + Room.getInstance().getPort(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS);
	}
}
