package room.handel;

import com.google.protobuf.Message;
import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;
import room.Room;
import room.connect.ConnectProcessor;

/**
 * 注册信息通知
 */
public class RegisterNoticeHandler implements Handler {

	private static final RegisterNoticeHandler instance = new RegisterNoticeHandler();

	public static RegisterNoticeHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, int aLong, Message registerInfo, int mapId, long sequence) {
		ModelProto.NotRegisterInfo req = (ModelProto.NotRegisterInfo) registerInfo;

		return Room.getInstance().getServerManager().connectToSever(req.getServersList(),
				Room.getInstance().getServerId(), Room.getInstance().getInnerIp() + "：" + Room.getInstance().getPort(),
				ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
				ConnectProcessor.HANDLERS, ServerType.Room);
	}
}
