package gate.client.handle.back;

import gate.client.GateTcpClient;
import msg.annotation.ProcessType;
import msg.registor.message.GMsg;
import net.message.TCPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GameProto;

/**
 * @author admin
 * @className JoinTableBack
 * @description
 * @createDate 2025/10/21 15:41
 */
@ProcessType(GMsg.ACK_ENTER_TABLE_MSG)
public class JoinTableBack implements BackHandle {

	private static final Logger logger = LoggerFactory.getLogger(JoinTableBack.class);

	@Override
	public void handle(TCPMessage response, GateTcpClient client) {
		try {
			GameProto.AckEnterTable res = GameProto.AckEnterTable.parseFrom(response.getMessage());
			client.setMapId(res.getTableInfo().getTableId());
			logger.info("用户加入game 桌子成功, userId: {}, mapId: {}", client.getRoleId(), client.getMapId());
		} catch (Exception e) {
			logger.error("解析加入game 桌子响应失败, msgId: {}, userId: {}", Integer.toHexString(response.getMessageId()), client.getRoleId(), e);
		}
	}
}
