package gate.client.handle.back;

import com.google.protobuf.Message;
import gate.client.GateTcpClient;
import msg.annotation.ProcessType;
import msg.registor.message.GMsg;
import net.client.Sender;
import net.handler.Handler;
import net.message.TCPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 离开桌子成功后清空 gate 上的 mapId，避免后续消息仍发往旧桌。
 */
@ProcessType(GMsg.ACK_LEAVE)
public class LeaveTableBack implements BackHandle, Handler {

	private static final Logger logger = LoggerFactory.getLogger(LeaveTableBack.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message msg, long mapId, int sequence) {
		return false;
	}

	@Override
	public void handle(TCPMessage response, GateTcpClient client) {
		try {
			client.setMapId(-1);
			logger.info("用户离开桌子成功, userId: {}, 已清空 mapId", client.getRoleId());
		} catch (Exception e) {
			logger.error("处理离开桌子响应失败, userId: {}", client.getRoleId(), e);
		}
	}
}
