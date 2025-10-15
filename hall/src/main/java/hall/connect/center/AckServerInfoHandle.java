package hall.connect.center;

import com.google.protobuf.Message;
import hall.Hall;
import hall.connect.ConnectProcessor;
import msg.annotation.ProcessType;
import msg.registor.enums.ServerType;
import msg.registor.message.CMsg;
import net.connect.handle.ConnectHandler;
import proto.ModelProto;
import utils.manager.ConnectHandle;
import utils.manager.HandleManager;

/**
 * 向中心请求大厅需要链接的服务信息回复
 */
@ProcessType(CMsg.ACK_SERVER)
public class AckServerInfoHandle implements ConnectHandle {

	@Override
	public void handle(Message message, ConnectHandler serverClient, int sequence) {
		if (message instanceof ModelProto.AckServerInfo) {
			ModelProto.AckServerInfo ack = (ModelProto.AckServerInfo) message;
			if (ack.getServersCount() > 0) {
				LOGGER.error("handle:{}", ack.getServers(0).getServerType() +
						" " + ack.getServers(0).getServerId() +
						" " + ack.getServers(0).getIpConfig().toStringUtf8());
				Hall.getInstance().execute(() -> Hall.getInstance().getServerManager().connectToSingleServer(
						ack.getServersList().get(0),
						Hall.getInstance().getServerId(),
						Hall.getInstance().getInnerIp() + ":" + Hall.getInstance().getPort(),
						ConnectProcessor.TRANSFER, ConnectProcessor.PARSER,
						ConnectProcessor.HANDLERS, ServerType.Hall, null));
			} else {
				//加入五秒重试机制
				Hall.getInstance().registerTimer(5000, 1000, 1, robot -> {
					HandleManager.sendMsg(CMsg.REQ_SERVER, ModelProto.ReqServerInfo.newBuilder()
							.addServerType(ServerType.Room.getServerType())
							.build(), serverClient, ConnectProcessor.PARSER);
					return true;
				}, Hall.getInstance());
			}
		}
	}
}
