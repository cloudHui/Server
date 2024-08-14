package room.handel;

import com.google.protobuf.Message;
import msg.MessageId;
import msg.ServerType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;
import room.Room;
import room.client.RoomClient;

/**
 * 注册服务信息请求
 */
public class ReqRegisterHandler implements Handler {

	private static final ReqRegisterHandler instance = new ReqRegisterHandler();

	public static ReqRegisterHandler getInstance() {
		return instance;
	}

	@Override
	public boolean handler(Sender sender, long aLong, Message msg, int mapId) {
		ModelProto.ReqRegister req = (ModelProto.ReqRegister) msg;
		ModelProto.ServerInfo serverInfo = req.getServerInfo();
		ServerType serverType = ServerType.get(serverInfo.getServerType());
		if (serverType == null) {
			return true;
		}
		RoomClient client = (RoomClient) sender;

		client.setServerInfo(serverInfo);


		Room.getInstance().serverClientManager.addServerClient(serverType, client, serverInfo.getServerId());

		ModelProto.AckRegister.Builder ackRegister = ModelProto.AckRegister.newBuilder();
		ackRegister.setServerInfo(serverInfo);
		sender.sendMessage(Math.toIntExact(aLong), MessageId.ACK_REGISTER, ackRegister.build(), null);
		return true;
	}
}
