package room.client.handle;

import com.google.protobuf.Message;
import msg.MessageId;
import msg.ServerType;
import msg.annotation.ProcessType;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;
import room.Room;
import room.client.RoomClient;

/**
 * 注册服务信息请求
 */
@ProcessType(MessageId.REQ_REGISTER)
public class ReqRegisterHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, int mapId, long sequence) {
		ModelProto.ReqRegister req = (ModelProto.ReqRegister) msg;
		ModelProto.ServerInfo serverInfo = req.getServerInfo();
		ServerType serverType = ServerType.get(serverInfo.getServerType());
		if (serverType == null) {
			return true;
		}
		RoomClient client = (RoomClient) sender;

		client.setServerInfo(serverInfo);

		Room.getInstance().getServerClientManager().addServerClient(serverType, client, serverInfo.getServerId());

		ModelProto.AckRegister.Builder ackRegister = ModelProto.AckRegister.newBuilder();
		ackRegister.setServerInfo(Room.getInstance().getServerInfo());
		sender.sendMessage(clientId, MessageId.ACK_REGISTER, mapId, 0, ackRegister.build(), sequence);
		return true;
	}
}
