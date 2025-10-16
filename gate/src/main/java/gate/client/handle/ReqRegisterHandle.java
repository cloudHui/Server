package gate.client.handle;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.client.Sender;
import net.handler.Handler;
import proto.ModelProto;

/**
 * 机器人注册服务信息请求
 */
@ProcessType(CMsg.REQ_REGISTER)
public class ReqRegisterHandle implements Handler {

	@Override
	public boolean handler(Sender sender, int clientId, Message reqRegister, int mapId, int sequence) {
		ModelProto.ReqRegister req = (ModelProto.ReqRegister) reqRegister;
		ModelProto.ServerInfo serverInfo = req.getServerInfo();
		ServerType serverType = ServerType.get(serverInfo.getServerType());
		if (serverType == null) {
			return true;
		}

		ModelProto.AckRegister.Builder ackRegister = ModelProto.AckRegister.newBuilder();
		sender.sendMessage(clientId, CMsg.ACK_REGISTER, mapId, ackRegister.build(), sequence);
		return true;
	}
}
