package hall.client.handle;

import com.google.protobuf.Message;
import msg.annotation.ProcessType;
import msg.registor.message.HMsg;
import net.client.Sender;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.HallProto;

/**
 * 处理加入工会请求
 * 负责工会成员管理
 */
@ProcessType(HMsg.REQ_JOIN_CLUB_MSG)
public class ReqJoinClubHandler implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(ReqJoinClubHandler.class);

	@Override
	public boolean handler(Sender sender, int clientId, Message message, int mapId, long sequence) {
		try {
			HallProto.ReqJoinClub request = (HallProto.ReqJoinClub) message;

			logger.info("处理加入工会请求, clientId: {}, clubId: {}",
					clientId, request.getClubId());

			// 构建响应
			HallProto.AckJoinClub.Builder response = buildJoinClubResponse();

			// 发送响应
			sender.sendMessage(clientId, HMsg.ACK_JOIN_CLUB_MSG, mapId, response.build(), sequence);

			logger.info("加入工会请求处理完成, clientId: {}", clientId);
			return true;
		} catch (Exception e) {
			logger.error("处理加入工会请求失败, clientId: {}", clientId, e);
			return false;
		}
	}

	/**
	 * 构建加入工会响应
	 */
	private HallProto.AckJoinClub.Builder buildJoinClubResponse() {
		// TODO: 实现实际的工会加入逻辑
		return HallProto.AckJoinClub.newBuilder();
	}
}